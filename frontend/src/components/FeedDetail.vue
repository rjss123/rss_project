<template>
  <div class="container">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else>
      <div class="feed-header">
        <h2>{{ feed.title }}</h2>
        <button @click="$router.back()" class="btn btn-primary">返回</button>
      </div>
      <p class="feed-url">{{ feed.url }}</p>
      <p class="feed-description">{{ feed.description }}</p>

      <h3>文章列表</h3>
      <div class="articles">
        <div v-for="article in feed.articles" :key="article.id" class="article-card">
          <h4 class="article-title">
            <a :href="article.link" target="_blank" rel="noopener">{{ article.title }}</a>
          </h4>
          <div class="article-meta">
            <span v-if="article.author">作者: {{ article.author }}</span>
            <span v-if="article.published">{{ formatDate(article.published) }}</span>
          </div>
          <p class="article-description" v-html="article.description"></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { feedAPI } from '../api'

export default {
  name: 'FeedDetail',
  data() {
    return {
      feed: null,
      loading: true,
      error: null
    }
  },
  async mounted() {
    await this.loadFeed()
  },
  methods: {
    async loadFeed() {
      try {
        this.loading = true
        const response = await feedAPI.getFeed(this.$route.params.id)
        this.feed = response.data
      } catch (err) {
        this.error = '加载订阅详情失败: ' + err.message
      } finally {
        this.loading = false
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
.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

h2 {
  color: #2c3e50;
}

h3 {
  margin: 2rem 0 1rem;
  color: #2c3e50;
}

.feed-url {
  font-size: 0.875rem;
  color: #7f8c8d;
  margin-bottom: 0.5rem;
  word-break: break-all;
}

.feed-description {
  color: #555;
  line-height: 1.6;
  margin-bottom: 1rem;
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

.article-title {
  margin-bottom: 0.5rem;
}

.article-title a {
  color: #2c3e50;
  text-decoration: none;
}

.article-title a:hover {
  color: #3498db;
  text-decoration: underline;
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
</style>
