package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"pocket-clone/internal/parser"
	"pocket-clone/internal/storage"
)

type Handler struct {
	db *storage.SQLiteDB
}

func New(db *storage.SQLiteDB) *Handler {
	return &Handler{db: db}
}

type CreateArticleRequest struct {
	URL string `json:"url"`
}

type UpdateArticleRequest struct {
	Archived *bool `json:"archived,omitempty"`
	MarkRead *bool `json:"mark_read,omitempty"`
}

type AddTagRequest struct {
	Tag string `json:"tag"`
}

func (h *Handler) CreateArticle(w http.ResponseWriter, r *http.Request) {
	var req CreateArticleRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.URL == "" {
		http.Error(w, "URL is required", http.StatusBadRequest)
		return
	}

	// Parse article content
	article, err := parser.Parse(req.URL)
	if err != nil {
		http.Error(w, "Failed to parse article: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Save to database
	id, err := h.db.CreateArticle(article)
	if err != nil {
		http.Error(w, "Failed to save article: "+err.Error(), http.StatusInternalServerError)
		return
	}

	article.ID = id
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(article)
}

func (h *Handler) GetArticle(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid article ID", http.StatusBadRequest)
		return
	}

	article, err := h.db.GetArticle(id)
	if err != nil {
		http.Error(w, "Article not found", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(article)
}

func (h *Handler) ListArticles(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()

	// Parse filters
	var archived *bool
	if a := query.Get("archived"); a != "" {
		val := a == "true"
		archived = &val
	}

	limit := 50
	if l := query.Get("limit"); l != "" {
		if parsed, err := strconv.Atoi(l); err == nil && parsed > 0 && parsed <= 100 {
			limit = parsed
		}
	}

	offset := 0
	if o := query.Get("offset"); o != "" {
		if parsed, err := strconv.Atoi(o); err == nil && parsed >= 0 {
			offset = parsed
		}
	}

	// Check for tag filter
	if tag := query.Get("tag"); tag != "" {
		articles, err := h.db.GetArticlesByTag(tag, limit, offset)
		if err != nil {
			http.Error(w, "Failed to fetch articles", http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(articles)
		return
	}

	articles, err := h.db.ListArticles(archived, limit, offset)
	if err != nil {
		http.Error(w, "Failed to fetch articles", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(articles)
}

func (h *Handler) UpdateArticle(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid article ID", http.StatusBadRequest)
		return
	}

	var req UpdateArticleRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if err := h.db.UpdateArticle(id, req.Archived, req.MarkRead); err != nil {
		http.Error(w, "Failed to update article", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) DeleteArticle(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid article ID", http.StatusBadRequest)
		return
	}

	if err := h.db.DeleteArticle(id); err != nil {
		http.Error(w, "Failed to delete article", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) Search(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query().Get("q")
	if query == "" {
		http.Error(w, "Search query is required", http.StatusBadRequest)
		return
	}

	limit := 20
	if l := r.URL.Query().Get("limit"); l != "" {
		if parsed, err := strconv.Atoi(l); err == nil && parsed > 0 && parsed <= 50 {
			limit = parsed
		}
	}

	articles, err := h.db.Search(query, limit)
	if err != nil {
		http.Error(w, "Search failed", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(articles)
}

func (h *Handler) ListTags(w http.ResponseWriter, r *http.Request) {
	tags, err := h.db.GetAllTags()
	if err != nil {
		http.Error(w, "Failed to fetch tags", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(tags)
}

func (h *Handler) AddTag(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	articleID, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid article ID", http.StatusBadRequest)
		return
	}

	var req AddTagRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.Tag == "" {
		http.Error(w, "Tag name is required", http.StatusBadRequest)
		return
	}

	tagID, err := h.db.CreateTag(req.Tag)
	if err != nil {
		http.Error(w, "Failed to create tag", http.StatusInternalServerError)
		return
	}

	if err := h.db.AddTagToArticle(articleID, tagID); err != nil {
		http.Error(w, "Failed to add tag to article", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
}

func (h *Handler) RemoveTag(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	articleID, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid article ID", http.StatusBadRequest)
		return
	}

	tagName := r.PathValue("tag")
	if tagName == "" {
		http.Error(w, "Tag name is required", http.StatusBadRequest)
		return
	}

	// Get tag ID
	tags, err := h.db.GetAllTags()
	if err != nil {
		http.Error(w, "Failed to fetch tags", http.StatusInternalServerError)
		return
	}

	var tagID int64
	for _, t := range tags {
		if t.Name == tagName {
			tagID = t.ID
			break
		}
	}

	if tagID == 0 {
		http.Error(w, "Tag not found", http.StatusNotFound)
		return
	}

	if err := h.db.RemoveTagFromArticle(articleID, tagID); err != nil {
		http.Error(w, "Failed to remove tag", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}
