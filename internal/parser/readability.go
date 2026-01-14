package parser

import (
	"net/http"
	"net/url"
	"strings"
	"time"

	readability "github.com/go-shiori/go-readability"
	"pocket-clone/internal/storage"
)

var httpClient = &http.Client{
	Timeout: 30 * time.Second,
}

// Parse fetches a URL and extracts the article content
func Parse(articleURL string) (*storage.Article, error) {
	// Validate URL
	parsedURL, err := url.Parse(articleURL)
	if err != nil {
		return nil, err
	}

	// Ensure scheme
	if parsedURL.Scheme == "" {
		parsedURL.Scheme = "https"
		articleURL = parsedURL.String()
	}

	// Fetch the page
	resp, err := httpClient.Get(articleURL)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	// Parse with readability
	article, err := readability.FromReader(resp.Body, parsedURL)
	if err != nil {
		return nil, err
	}

	// Extract plain text for search
	textContent := extractText(article.TextContent)

	// Create excerpt if not provided
	excerpt := article.Excerpt
	if excerpt == "" && len(textContent) > 0 {
		if len(textContent) > 200 {
			excerpt = textContent[:200] + "..."
		} else {
			excerpt = textContent
		}
	}

	return &storage.Article{
		URL:         articleURL,
		Title:       article.Title,
		Content:     article.Content,
		TextContent: textContent,
		Excerpt:     excerpt,
		Author:      article.Byline,
		ImageURL:    article.Image,
		SavedAt:     time.Now(),
	}, nil
}

// extractText cleans up text content
func extractText(text string) string {
	// Normalize whitespace
	lines := strings.Split(text, "\n")
	var cleaned []string
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line != "" {
			cleaned = append(cleaned, line)
		}
	}
	return strings.Join(cleaned, "\n")
}
