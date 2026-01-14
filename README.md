# Pocket Clone

A self-hosted read-it-later app inspired by Pocket. Save articles from the web, read them in a clean reader view, and organize with tags.

## Features

- **Save articles** - Automatically extracts content, title, and images from any URL
- **Reader mode** - Clean, distraction-free reading experience
- **Full-text search** - Find articles by content with highlighted snippets
- **Tags** - Organize articles with custom tags
- **Archive** - Keep your reading list clean without deleting
- **Offline support** - PWA with service worker caching
- **Dark mode** - Respects system preference
- **Chrome extension** - Save articles with one click

## Quick Start

### Using Docker

```bash
docker compose up -d
```

Open http://localhost:8080

### From Source

Requires Go 1.23+ with CGO enabled.

```bash
make build
./pocket-clone
```

## Usage

### Web Interface

- Click **+ Add** to save an article by URL
- Click an article to open reader view
- Use the search bar to find articles
- Click **Archive** to move articles out of your main list
- Switch between **Unread**, **Archive**, and **Tags** views

### Chrome Extension

1. Open `chrome://extensions`
2. Enable **Developer mode**
3. Click **Load unpacked** and select the `extension` folder
4. Click the extension icon and configure your server URL
5. On any webpage, click the extension icon to save

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/articles` | Save article `{"url": "..."}` |
| GET | `/api/articles` | List articles (query: `archived`, `tag`, `limit`, `offset`) |
| GET | `/api/articles/{id}` | Get single article |
| PATCH | `/api/articles/{id}` | Update article `{"archived": bool}` |
| DELETE | `/api/articles/{id}` | Delete article |
| GET | `/api/search?q=` | Full-text search |
| GET | `/api/tags` | List all tags |
| POST | `/api/articles/{id}/tags` | Add tag `{"tag": "..."}` |
| DELETE | `/api/articles/{id}/tags/{tag}` | Remove tag |

## Configuration

| Flag | Default | Description |
|------|---------|-------------|
| `-port` | 8080 | HTTP server port |
| `-db` | pocket.db | SQLite database path |

## Project Structure

```
pocket-clone/
├── main.go                 # Entry point
├── internal/
│   ├── handlers/           # HTTP handlers
│   ├── parser/             # Article content extraction
│   ├── server/             # HTTP server setup
│   └── storage/            # SQLite database layer
├── web/                    # Frontend (HTML/CSS/JS)
├── extension/              # Chrome extension
├── Dockerfile
└── docker-compose.yml
```

## Tech Stack

- **Backend**: Go, SQLite with FTS5
- **Frontend**: Vanilla JavaScript, CSS (no frameworks)
- **Parser**: go-readability for content extraction
- **Container**: Multi-stage Docker build with static linking

## License

MIT
