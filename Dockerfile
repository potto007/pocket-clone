# Build stage
FROM golang:1.23-alpine AS builder

RUN apk add --no-cache gcc musl-dev sqlite-dev

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=1 go build -tags "fts5" -ldflags="-s -w" -o pocket-clone .

# Runtime stage
FROM alpine:3.20

RUN apk add --no-cache ca-certificates sqlite-libs

WORKDIR /app

COPY --from=builder /app/pocket-clone .
COPY --from=builder /app/web ./web

RUN mkdir -p /data

EXPOSE 8080

ENTRYPOINT ["./pocket-clone"]
CMD ["-port", "8080", "-db", "/data/pocket.db"]
