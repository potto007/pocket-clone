package server

import (
	"context"
	"net/http"
	"time"

	"pocket-clone/internal/handlers"
	"pocket-clone/internal/storage"
)

type Server struct {
	httpServer *http.Server
	db         *storage.SQLiteDB
}

func New(db *storage.SQLiteDB, port string) *Server {
	s := &Server{db: db}

	mux := http.NewServeMux()
	h := handlers.New(db)

	// API routes
	mux.HandleFunc("POST /api/articles", h.CreateArticle)
	mux.HandleFunc("GET /api/articles", h.ListArticles)
	mux.HandleFunc("GET /api/articles/{id}", h.GetArticle)
	mux.HandleFunc("PATCH /api/articles/{id}", h.UpdateArticle)
	mux.HandleFunc("DELETE /api/articles/{id}", h.DeleteArticle)
	mux.HandleFunc("GET /api/search", h.Search)
	mux.HandleFunc("GET /api/tags", h.ListTags)
	mux.HandleFunc("POST /api/articles/{id}/tags", h.AddTag)
	mux.HandleFunc("DELETE /api/articles/{id}/tags/{tag}", h.RemoveTag)

	// Static files
	mux.Handle("/", http.FileServer(http.Dir("web")))

	s.httpServer = &http.Server{
		Addr:         ":" + port,
		Handler:      corsMiddleware(mux),
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	return s
}

func (s *Server) Start() error {
	return s.httpServer.ListenAndServe()
}

func (s *Server) Shutdown() error {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	return s.httpServer.Shutdown(ctx)
}

func corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}
