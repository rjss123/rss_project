<template>
  <div class="container">
    <div class="header">
      <h2>最新文章</h2>
      <button @click="showFavorites = !showFavorites" class="btn btn-primary">
        {{ showFavorites ? '显示全部' : '显示收藏' }}
      </button>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="articles">
      <div v-for="article in articles" :key="article.id" class="article-card">
        <div class="article-header">
          <h3 class="article-title">
            <a :href="article.link" target="_blank" rel="noopener">{{ article.title }}</a>
          </h3>
          <div class="article-actions">
            <button
              @click="summarizeArticle(article)"
              class="btn-summary"
              :disabled="article.summarizing">
              {{ article.summarizing ? '总结中...' : (article.summary ? '隐藏总结' : 'AI总结') }}
            </button>
            <button
              @click="translateArticle(article)"
              class="btn-translate"
              :disabled="article.translating">
              {{ article.translating ? '翻译中...' : '翻译' }}
            </button>
            <button
              @click="toggleFavorite(article)"
              class="btn-favorite"
              :class="{ 'favorited': article.is_favorited }">
              {{ article.is_favorited ? '★' : '☆' }}
            </button>
          </div>
        </div>
        <div class="article-meta">
          <span v-if="article.author">作者: {{ article.author }}</span>
          <span v-if="article.published">{{ formatDate(article.published) }}</span>
        </div>
        <p class="article-description" v-html="article.description"></p>
        <div v-if="article.summary" class="article-summary">
          <div class="summary-header">AI 总结：</div>
          <p class="summary-content">{{ article.summary }}</p>
        </div>
        <div v-if="article.translated" class="article-translation">
          <div class="translation-header">🌐 中文翻译：</div>
          <p class="translation-content" v-html="article.translated"></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { feedAPI } from '../api'

export default {
  name: 'ArticleList',
  data() {
    return {
      articles: [],
      loading: true,
      error: null,
      showFavorites: false
    }
  },
  async mounted() {
    await this.loadArticles()
  },
  watch: {
    showFavorites() {
      this.loadArticles()
    }
  },
  methods: {
    async loadArticles() {
      try {
        this.loading = true
        const response = this.showFavorites
          ? await feedAPI.getFavorites()
          : await feedAPI.getArticles()
        this.articles = response.data.map(article => ({
          ...article,
          translating: false,
          translated: null,
          summarizing: false,
          summary: null
        }))
      } catch (err) {
        this.error = '加载文章失败: ' + err.message
      } finally {
        this.loading = false
      }
    },
    async summarizeArticle(article) {
      if (article.summary) {
        article.summary = null
        return
      }

      try {
        article.summarizing = true
        const text = `${article.title}\n\n${article.content || article.description || ''}`
        const response = await feedAPI.summarize({
          text,
          url: article.link
        })
        article.summary = response.data.summary
      } catch (err) {
        this.error = 'AI总结失败: ' + (err.response?.data?.detail || err.message)
      } finally {
        article.summarizing = false
      }
    },
    async translateArticle(article) {
      if (article.translated) {
        article.translated = null
        return
      }

      try {
        article.translating = true
        const textToTranslate = `${article.title}\n\n${article.description || ''}`
        const response = await feedAPI.translate(textToTranslate)
        article.translated = response.data.translated
      } catch (err) {
        this.error = '翻译失败: ' + err.message
      } finally {
        article.translating = false
      }
    },
    async toggleFavorite(article) {
      try {
        await feedAPI.toggleFavorite(article.id)
        article.is_favorited = !article.is_favorited
        if (this.showFavorites && !article.is_favorited) {
          this.articles = this.articles.filter(a => a.id !== article.id)
        }
      } catch (err) {
        this.error = '收藏操作失败: ' + err.message
      }
    },
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

h2 {
  color: #2c3e50;
  margin: 0;
}

.articles {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.article-card {
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  transition: box-shadow 0.3s;
}

.article-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.article-title {
  margin-bottom: 0.5rem;
  flex: 1;
}

.article-title a {
  color: #2c3e50;
  text-decoration: none;
}

.article-title a:hover {
  color: #3498db;
  text-decoration: underline;
}

.article-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.btn-summary {
  padding: 0.25rem 0.75rem;
  background: #27ae60;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background 0.3s;
}

.btn-summary:hover:not(:disabled) {
  background: #219653;
}

.btn-summary:disabled {
  background: #95a5a6;
  cursor: not-allowed;
}

.btn-translate {
  padding: 0.25rem 0.75rem;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background 0.3s;
}

.btn-translate:hover:not(:disabled) {
  background: #2980b9;
}

.btn-translate:disabled {
  background: #95a5a6;
  cursor: not-allowed;
}

.btn-favorite {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #ccc;
  transition: color 0.3s;
  padding: 0;
  line-height: 1;
}

.btn-favorite:hover {
  color: #f39c12;
}

.btn-favorite.favorited {
  color: #f39c12;
}

.article-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: #7f8c8d;
  margin-bottom: 0.5rem;
}

.article-description {
  color: #555;
  line-height: 1.6;
}

.article-summary {
  margin-top: 1rem;
  padding: 1rem;
  background: #eefaf1;
  border-left: 4px solid #27ae60;
  border-radius: 4px;
}

.summary-header {
  font-weight: bold;
  color: #219653;
  margin-bottom: 0.5rem;
}

.summary-content {
  color: #2c3e50;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0;
}

.article-translation {
  margin-top: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-left: 4px solid #3498db;
  border-radius: 4px;
}

.translation-header {
  font-weight: bold;
  color: #3498db;
  margin-bottom: 0.5rem;
}

.translation-content {
  color: #2c3e50;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
