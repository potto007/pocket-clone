// API client for Pocket Clone

const API = {
    baseUrl: '/api',

    async request(endpoint, options = {}) {
        const url = this.baseUrl + endpoint;
        const config = {
            headers: {
                'Content-Type': 'application/json',
            },
            ...options,
        };

        const response = await fetch(url, config);

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || `HTTP ${response.status}`);
        }

        if (response.status === 204) {
            return null;
        }

        return response.json();
    },

    // Articles
    async createArticle(url) {
        return this.request('/articles', {
            method: 'POST',
            body: JSON.stringify({ url }),
        });
    },

    async getArticle(id) {
        return this.request(`/articles/${id}`);
    },

    async listArticles(params = {}) {
        const query = new URLSearchParams();
        if (params.archived !== undefined) query.set('archived', params.archived);
        if (params.tag) query.set('tag', params.tag);
        if (params.limit) query.set('limit', params.limit);
        if (params.offset) query.set('offset', params.offset);

        const queryStr = query.toString();
        return this.request('/articles' + (queryStr ? '?' + queryStr : ''));
    },

    async updateArticle(id, updates) {
        return this.request(`/articles/${id}`, {
            method: 'PATCH',
            body: JSON.stringify(updates),
        });
    },

    async deleteArticle(id) {
        return this.request(`/articles/${id}`, {
            method: 'DELETE',
        });
    },

    // Search
    async search(query, limit = 20) {
        return this.request(`/search?q=${encodeURIComponent(query)}&limit=${limit}`);
    },

    // Tags
    async listTags() {
        return this.request('/tags');
    },

    async addTag(articleId, tag) {
        return this.request(`/articles/${articleId}/tags`, {
            method: 'POST',
            body: JSON.stringify({ tag }),
        });
    },

    async removeTag(articleId, tag) {
        return this.request(`/articles/${articleId}/tags/${encodeURIComponent(tag)}`, {
            method: 'DELETE',
        });
    },
};
