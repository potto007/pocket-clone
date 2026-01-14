.PHONY: build run clean dev docker-build docker-run docker-stop

# Build the application (fts5 tag enables full-text search)
build:
	CGO_ENABLED=1 go build -tags "fts5" -o pocket-clone .

# Run the application
run: build
	./pocket-clone

# Run in development mode
dev:
	CGO_ENABLED=1 go run -tags "fts5" .

# Clean build artifacts
clean:
	rm -f pocket-clone pocket.db

# Build for multiple platforms
build-all:
	CGO_ENABLED=1 GOOS=linux GOARCH=amd64 go build -tags "fts5" -o pocket-clone-linux-amd64 .
	CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build -tags "fts5" -o pocket-clone-darwin-amd64 .
	CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build -tags "fts5" -o pocket-clone-darwin-arm64 .

# Docker commands
docker-build:
	docker build -t pocket-clone .

docker-run:
	docker compose up -d

docker-stop:
	docker compose down
