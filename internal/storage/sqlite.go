package storage

import (
	"database/sql"
	"time"

	_ "github.com/mattn/go-sqlite3"
)

type Article struct {
	ID          int64      `json:"id"`
	URL         string     `json:"url"`
	Title       string     `json:"title"`
	Content     string     `json:"content"`
	TextContent string     `json:"text_content,omitempty"`
	Excerpt     string     `json:"excerpt"`
	Author      string     `json:"author,omitempty"`
	ImageURL    string     `json:"image_url,omitempty"`
	SavedAt     time.Time  `json:"saved_at"`
	ReadAt      *time.Time `json:"read_at,omitempty"`
	Archived    bool       `json:"archived"`
	Tags        []string   `json:"tags,omitempty"`
}

type Tag struct {
	ID   int64  `json:"id"`
	Name string `json:"name"`
}

type SQLiteDB struct {
	db *sql.DB
}

func NewSQLiteDB(path string) (*SQLiteDB, error) {
	db, err := sql.Open("sqlite3", path+"?_foreign_keys=on")
	if err != nil {
		return nil, err
	}

	if err := db.Ping(); err != nil {
		return nil, err
	}

	return &SQLiteDB{db: db}, nil
}

func (s *SQLiteDB) Close() error {
	return s.db.Close()
}

func (s *SQLiteDB) Migrate() error {
	migrations := []string{
		`CREATE TABLE IF NOT EXISTS articles (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			url TEXT UNIQUE NOT NULL,
			title TEXT,
			content TEXT,
			text_content TEXT,
			excerpt TEXT,
			author TEXT,
			image_url TEXT,
			saved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			read_at DATETIME,
			archived INTEGER DEFAULT 0
		)`,
		`CREATE TABLE IF NOT EXISTS tags (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			name TEXT UNIQUE NOT NULL
		)`,
		`CREATE TABLE IF NOT EXISTS article_tags (
			article_id INTEGER REFERENCES articles(id) ON DELETE CASCADE,
			tag_id INTEGER REFERENCES tags(id) ON DELETE CASCADE,
			PRIMARY KEY (article_id, tag_id)
		)`,
		`CREATE VIRTUAL TABLE IF NOT EXISTS articles_fts USING fts5(
			title, text_content, content='articles', content_rowid='id'
		)`,
		// Triggers to keep FTS in sync
		`CREATE TRIGGER IF NOT EXISTS articles_ai AFTER INSERT ON articles BEGIN
			INSERT INTO articles_fts(rowid, title, text_content) VALUES (new.id, new.title, new.text_content);
		END`,
		`CREATE TRIGGER IF NOT EXISTS articles_ad AFTER DELETE ON articles BEGIN
			INSERT INTO articles_fts(articles_fts, rowid, title, text_content) VALUES('delete', old.id, old.title, old.text_content);
		END`,
		`CREATE TRIGGER IF NOT EXISTS articles_au AFTER UPDATE ON articles BEGIN
			INSERT INTO articles_fts(articles_fts, rowid, title, text_content) VALUES('delete', old.id, old.title, old.text_content);
			INSERT INTO articles_fts(rowid, title, text_content) VALUES (new.id, new.title, new.text_content);
		END`,
	}

	for _, m := range migrations {
		if _, err := s.db.Exec(m); err != nil {
			return err
		}
	}

	return nil
}

// CreateArticle saves a new article and returns its ID
func (s *SQLiteDB) CreateArticle(article *Article) (int64, error) {
	result, err := s.db.Exec(`
		INSERT INTO articles (url, title, content, text_content, excerpt, author, image_url)
		VALUES (?, ?, ?, ?, ?, ?, ?)
	`, article.URL, article.Title, article.Content, article.TextContent, article.Excerpt, article.Author, article.ImageURL)
	if err != nil {
		return 0, err
	}

	return result.LastInsertId()
}

// GetArticle retrieves a single article by ID
func (s *SQLiteDB) GetArticle(id int64) (*Article, error) {
	article := &Article{}
	var archived int
	var readAt sql.NullTime

	err := s.db.QueryRow(`
		SELECT id, url, title, content, text_content, excerpt, author, image_url, saved_at, read_at, archived
		FROM articles WHERE id = ?
	`, id).Scan(
		&article.ID, &article.URL, &article.Title, &article.Content, &article.TextContent,
		&article.Excerpt, &article.Author, &article.ImageURL, &article.SavedAt, &readAt, &archived,
	)
	if err != nil {
		return nil, err
	}

	article.Archived = archived == 1
	if readAt.Valid {
		article.ReadAt = &readAt.Time
	}

	// Get tags
	tags, err := s.GetArticleTags(id)
	if err != nil {
		return nil, err
	}
	article.Tags = tags

	return article, nil
}

// ListArticles returns articles with optional filtering
func (s *SQLiteDB) ListArticles(archived *bool, limit, offset int) ([]Article, error) {
	query := `
		SELECT id, url, title, excerpt, author, image_url, saved_at, read_at, archived
		FROM articles
	`
	args := []interface{}{}

	if archived != nil {
		if *archived {
			query += " WHERE archived = 1"
		} else {
			query += " WHERE archived = 0"
		}
	}

	query += " ORDER BY saved_at DESC LIMIT ? OFFSET ?"
	args = append(args, limit, offset)

	rows, err := s.db.Query(query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var articles []Article
	for rows.Next() {
		var a Article
		var archived int
		var readAt sql.NullTime

		err := rows.Scan(&a.ID, &a.URL, &a.Title, &a.Excerpt, &a.Author, &a.ImageURL, &a.SavedAt, &readAt, &archived)
		if err != nil {
			return nil, err
		}

		a.Archived = archived == 1
		if readAt.Valid {
			a.ReadAt = &readAt.Time
		}

		articles = append(articles, a)
	}

	return articles, nil
}

// UpdateArticle updates an article's archived or read status
func (s *SQLiteDB) UpdateArticle(id int64, archived *bool, markRead *bool) error {
	if archived != nil {
		archivedInt := 0
		if *archived {
			archivedInt = 1
		}
		if _, err := s.db.Exec("UPDATE articles SET archived = ? WHERE id = ?", archivedInt, id); err != nil {
			return err
		}
	}

	if markRead != nil && *markRead {
		if _, err := s.db.Exec("UPDATE articles SET read_at = CURRENT_TIMESTAMP WHERE id = ?", id); err != nil {
			return err
		}
	}

	return nil
}

// DeleteArticle removes an article
func (s *SQLiteDB) DeleteArticle(id int64) error {
	_, err := s.db.Exec("DELETE FROM articles WHERE id = ?", id)
	return err
}

// Search performs full-text search on articles
func (s *SQLiteDB) Search(query string, limit int) ([]Article, error) {
	rows, err := s.db.Query(`
		SELECT a.id, a.url, a.title, a.excerpt, a.author, a.image_url, a.saved_at, a.read_at, a.archived,
			   snippet(articles_fts, 1, '<mark>', '</mark>', '...', 32) as snippet
		FROM articles_fts f
		JOIN articles a ON a.id = f.rowid
		WHERE articles_fts MATCH ?
		ORDER BY rank
		LIMIT ?
	`, query, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var articles []Article
	for rows.Next() {
		var a Article
		var archived int
		var readAt sql.NullTime
		var snippet string

		err := rows.Scan(&a.ID, &a.URL, &a.Title, &a.Excerpt, &a.Author, &a.ImageURL, &a.SavedAt, &readAt, &archived, &snippet)
		if err != nil {
			return nil, err
		}

		a.Archived = archived == 1
		if readAt.Valid {
			a.ReadAt = &readAt.Time
		}
		// Use snippet as excerpt for search results
		if snippet != "" {
			a.Excerpt = snippet
		}

		articles = append(articles, a)
	}

	return articles, nil
}

// Tag operations

func (s *SQLiteDB) CreateTag(name string) (int64, error) {
	result, err := s.db.Exec("INSERT OR IGNORE INTO tags (name) VALUES (?)", name)
	if err != nil {
		return 0, err
	}

	id, err := result.LastInsertId()
	if err != nil || id == 0 {
		// Tag already exists, get its ID
		err = s.db.QueryRow("SELECT id FROM tags WHERE name = ?", name).Scan(&id)
		if err != nil {
			return 0, err
		}
	}

	return id, nil
}

func (s *SQLiteDB) GetAllTags() ([]Tag, error) {
	rows, err := s.db.Query("SELECT id, name FROM tags ORDER BY name")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var tags []Tag
	for rows.Next() {
		var t Tag
		if err := rows.Scan(&t.ID, &t.Name); err != nil {
			return nil, err
		}
		tags = append(tags, t)
	}

	return tags, nil
}

func (s *SQLiteDB) AddTagToArticle(articleID, tagID int64) error {
	_, err := s.db.Exec("INSERT OR IGNORE INTO article_tags (article_id, tag_id) VALUES (?, ?)", articleID, tagID)
	return err
}

func (s *SQLiteDB) RemoveTagFromArticle(articleID, tagID int64) error {
	_, err := s.db.Exec("DELETE FROM article_tags WHERE article_id = ? AND tag_id = ?", articleID, tagID)
	return err
}

func (s *SQLiteDB) GetArticleTags(articleID int64) ([]string, error) {
	rows, err := s.db.Query(`
		SELECT t.name FROM tags t
		JOIN article_tags at ON at.tag_id = t.id
		WHERE at.article_id = ?
		ORDER BY t.name
	`, articleID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var tags []string
	for rows.Next() {
		var name string
		if err := rows.Scan(&name); err != nil {
			return nil, err
		}
		tags = append(tags, name)
	}

	return tags, nil
}

func (s *SQLiteDB) GetArticlesByTag(tagName string, limit, offset int) ([]Article, error) {
	rows, err := s.db.Query(`
		SELECT a.id, a.url, a.title, a.excerpt, a.author, a.image_url, a.saved_at, a.read_at, a.archived
		FROM articles a
		JOIN article_tags at ON at.article_id = a.id
		JOIN tags t ON t.id = at.tag_id
		WHERE t.name = ?
		ORDER BY a.saved_at DESC
		LIMIT ? OFFSET ?
	`, tagName, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var articles []Article
	for rows.Next() {
		var a Article
		var archived int
		var readAt sql.NullTime

		err := rows.Scan(&a.ID, &a.URL, &a.Title, &a.Excerpt, &a.Author, &a.ImageURL, &a.SavedAt, &readAt, &archived)
		if err != nil {
			return nil, err
		}

		a.Archived = archived == 1
		if readAt.Valid {
			a.ReadAt = &readAt.Time
		}

		articles = append(articles, a)
	}

	return articles, nil
}
