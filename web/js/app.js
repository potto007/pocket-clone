// Pocket Clone main application

const App = {
    currentView: 'unread',
    currentArticle: null,
    articles: [],
    tags: [],

    init() {
        this.bindEvents();
        this.loadArticles();
        this.checkOnlineStatus();
    },

    bindEvents() {
        // Navigation
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const view = e.target.dataset.view;
                this.switchView(view);
            });
        });

        // Add article
        document.getElementById('add-btn').addEventListener('click', () => {
            this.showAddModal();
        });

        document.getElementById('add-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleAddArticle();
        });

        document.getElementById('cancel-add').addEventListener('click', () => {
            this.hideAddModal();
        });

        // Close modal on backdrop click
        document.getElementById('add-modal').addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) {
                this.hideAddModal();
            }
        });

        // Search
        let searchTimeout;
        document.getElementById('search-input').addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            const query = e.target.value.trim();

            searchTimeout = setTimeout(() => {
                if (query.length >= 2) {
                    this.handleSearch(query);
                } else if (query.length === 0) {
                    this.loadArticles();
                }
            }, 300);
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            // Escape to close reader or modal
            if (e.key === 'Escape') {
                if (this.currentArticle) {
                    this.closeReader();
                } else {
                    this.hideAddModal();
                }
            }
            // 'a' to add (when not in input)
            if (e.key === 'a' && !e.target.matches('input, textarea')) {
                e.preventDefault();
                this.showAddModal();
            }
            // '/' to focus search
            if (e.key === '/' && !e.target.matches('input, textarea')) {
                e.preventDefault();
                document.getElementById('search-input').focus();
            }
        });

        // Online/offline status
        window.addEventListener('online', () => this.updateOnlineStatus(true));
        window.addEventListener('offline', () => this.updateOnlineStatus(false));
    },

    async switchView(view) {
        this.currentView = view;
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === view);
        });

        if (view === 'tags') {
            await this.loadTags();
        } else {
            await this.loadArticles();
        }
    },

    async loadArticles() {
        const listEl = document.getElementById('article-list');
        listEl.innerHTML = '<div class="loading"><span class="spinner"></span> Loading...</div>';

        try {
            const archived = this.currentView === 'archive';
            this.articles = await API.listArticles({ archived }) || [];
            this.renderArticles();
        } catch (error) {
            console.error('Failed to load articles:', error);
            listEl.innerHTML = '<div class="empty-state"><h3>Failed to load articles</h3><p>Please try again later.</p></div>';
        }
    },

    async loadTags() {
        const listEl = document.getElementById('article-list');
        listEl.innerHTML = '<div class="loading"><span class="spinner"></span> Loading...</div>';

        try {
            this.tags = await API.listTags() || [];
            this.renderTags();
        } catch (error) {
            console.error('Failed to load tags:', error);
            listEl.innerHTML = '<div class="empty-state"><h3>Failed to load tags</h3></div>';
        }
    },

    renderArticles() {
        const listEl = document.getElementById('article-list');

        if (!this.articles || this.articles.length === 0) {
            const message = this.currentView === 'archive'
                ? 'No archived articles yet'
                : 'No articles saved. Click "+ Add" to save your first article!';
            listEl.innerHTML = `<div class="empty-state"><h3>${message}</h3></div>`;
            return;
        }

        listEl.innerHTML = this.articles.map(article => this.renderArticleCard(article)).join('');

        // Bind click events
        listEl.querySelectorAll('.article-card').forEach(card => {
            card.addEventListener('click', (e) => {
                if (!e.target.closest('.article-actions')) {
                    const id = parseInt(card.dataset.id);
                    this.openArticle(id);
                }
            });
        });

        // Bind action buttons
        listEl.querySelectorAll('[data-action]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const action = btn.dataset.action;
                const id = parseInt(btn.closest('.article-card').dataset.id);
                this.handleArticleAction(action, id);
            });
        });
    },

    renderArticleCard(article) {
        const date = new Date(article.saved_at).toLocaleDateString();
        const imageHtml = article.image_url
            ? `<img src="${article.image_url}" alt="" class="article-image" loading="lazy">`
            : '<div class="article-image"></div>';

        const archiveBtn = this.currentView === 'archive'
            ? '<button class="btn btn-icon" data-action="unarchive" title="Unarchive">📥</button>'
            : '<button class="btn btn-icon" data-action="archive" title="Archive">📦</button>';

        return `
            <article class="article-card" data-id="${article.id}">
                ${imageHtml}
                <div class="article-content">
                    <h2 class="article-title">${this.escapeHtml(article.title || 'Untitled')}</h2>
                    <p class="article-excerpt">${this.escapeHtml(article.excerpt || '')}</p>
                    <div class="article-meta">
                        <span>${date}</span>
                        ${article.author ? `<span>by ${this.escapeHtml(article.author)}</span>` : ''}
                        <div class="article-actions">
                            ${archiveBtn}
                            <button class="btn btn-icon" data-action="delete" title="Delete">🗑️</button>
                        </div>
                    </div>
                </div>
            </article>
        `;
    },

    renderTags() {
        const listEl = document.getElementById('article-list');

        if (!this.tags || this.tags.length === 0) {
            listEl.innerHTML = '<div class="empty-state"><h3>No tags yet</h3><p>Add tags to your articles to organize them.</p></div>';
            return;
        }

        listEl.innerHTML = `
            <div class="tags-list" style="padding: 1rem;">
                ${this.tags.map(tag => `
                    <button class="tag" data-tag="${this.escapeHtml(tag.name)}" style="cursor: pointer; padding: 0.5rem 1rem; font-size: 1rem;">
                        ${this.escapeHtml(tag.name)}
                    </button>
                `).join('')}
            </div>
        `;

        listEl.querySelectorAll('[data-tag]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const tag = btn.dataset.tag;
                this.articles = await API.listArticles({ tag }) || [];
                this.renderArticles();
            });
        });
    },

    async openArticle(id) {
        try {
            this.currentArticle = await API.getArticle(id);
            this.renderReader();

            // Mark as read
            await API.updateArticle(id, { mark_read: true });
        } catch (error) {
            console.error('Failed to load article:', error);
            alert('Failed to load article');
        }
    },

    renderReader() {
        const readerEl = document.getElementById('reader');
        const listEl = document.getElementById('article-list');
        const article = this.currentArticle;

        const date = new Date(article.saved_at).toLocaleDateString();

        readerEl.innerHTML = `
            <div class="reader-header">
                <a class="reader-back" id="reader-back">← Back to list</a>
                <h1 class="reader-title">${this.escapeHtml(article.title || 'Untitled')}</h1>
                <div class="reader-meta">
                    ${article.author ? `By ${this.escapeHtml(article.author)} • ` : ''}
                    Saved ${date}
                    ${article.url ? ` • <a href="${article.url}" target="_blank" rel="noopener">Original</a>` : ''}
                </div>
            </div>
            <div class="reader-content">
                ${article.content}
            </div>
        `;

        document.getElementById('reader-back').addEventListener('click', () => {
            this.closeReader();
        });

        listEl.classList.add('hidden');
        readerEl.classList.remove('hidden');
        window.scrollTo(0, 0);
    },

    closeReader() {
        this.currentArticle = null;
        document.getElementById('reader').classList.add('hidden');
        document.getElementById('article-list').classList.remove('hidden');
    },

    async handleArticleAction(action, id) {
        try {
            switch (action) {
                case 'archive':
                    await API.updateArticle(id, { archived: true });
                    this.articles = this.articles.filter(a => a.id !== id);
                    this.renderArticles();
                    break;
                case 'unarchive':
                    await API.updateArticle(id, { archived: false });
                    this.articles = this.articles.filter(a => a.id !== id);
                    this.renderArticles();
                    break;
                case 'delete':
                    if (confirm('Delete this article?')) {
                        await API.deleteArticle(id);
                        this.articles = this.articles.filter(a => a.id !== id);
                        this.renderArticles();
                    }
                    break;
            }
        } catch (error) {
            console.error('Action failed:', error);
            alert('Action failed. Please try again.');
        }
    },

    showAddModal() {
        document.getElementById('add-modal').classList.remove('hidden');
        document.getElementById('url-input').focus();
    },

    hideAddModal() {
        document.getElementById('add-modal').classList.add('hidden');
        document.getElementById('url-input').value = '';
    },

    async handleAddArticle() {
        const urlInput = document.getElementById('url-input');
        const url = urlInput.value.trim();

        if (!url) return;

        const submitBtn = document.querySelector('#add-form button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Saving...';

        try {
            const article = await API.createArticle(url);
            this.hideAddModal();

            // Refresh list if on unread view
            if (this.currentView === 'unread') {
                this.articles.unshift(article);
                this.renderArticles();
            }
        } catch (error) {
            console.error('Failed to save article:', error);
            alert('Failed to save article: ' + error.message);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Save';
        }
    },

    async handleSearch(query) {
        const listEl = document.getElementById('article-list');
        listEl.innerHTML = '<div class="loading"><span class="spinner"></span> Searching...</div>';

        try {
            this.articles = await API.search(query) || [];
            this.renderArticles();
        } catch (error) {
            console.error('Search failed:', error);
            listEl.innerHTML = '<div class="empty-state"><h3>Search failed</h3></div>';
        }
    },

    checkOnlineStatus() {
        this.updateOnlineStatus(navigator.onLine);
    },

    updateOnlineStatus(isOnline) {
        const indicator = document.getElementById('offline-indicator');
        indicator.classList.toggle('hidden', isOnline);
    },

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => App.init());
