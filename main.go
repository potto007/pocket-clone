package main

import (
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"

	"pocket-clone/internal/server"
	"pocket-clone/internal/storage"
)

func main() {
	port := flag.String("port", "8080", "Server port")
	dbPath := flag.String("db", "./pocket.db", "Database file path")
	flag.Parse()

	// Initialize database
	db, err := storage.NewSQLiteDB(*dbPath)
	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}
	defer db.Close()

	// Run migrations
	if err := db.Migrate(); err != nil {
		log.Fatalf("Failed to run migrations: %v", err)
	}

	// Create and start server
	srv := server.New(db, *port)

	// Handle graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		<-quit
		log.Println("Shutting down server...")
		srv.Shutdown()
	}()

	log.Printf("Starting server on http://localhost:%s", *port)
	if err := srv.Start(); err != nil {
		log.Fatalf("Server error: %v", err)
	}
}
