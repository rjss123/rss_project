<template>
  <div class="container">
    <div class="header">
      <h2>订阅管理</h2>
      <button @click="showAddForm = !showAddForm" class="btn btn-primary">
        {{ showAddForm ? '取消' : '添加订阅' }}
      </button>
    </div>

    <div v-if="showAddForm" class="add-form">
      <input
        v-model="newFeedUrl"
        type="url"
        placeholder="输入 RSS 源 URL"
        @keyup.enter="addFeed"
      />
      <button @click="addFeed" class="btn btn-success" :disabled="adding">
        {{ adding ? '添加中...' : '确认' }}
      </button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="feeds">
      <div v-for="feed in feeds" :key="feed.id" class="feed-card">
        <div class="feed-info">
          <h3 class="feed-title">
            <router-link :to="`/feeds/${feed.id}`">{{ feed.title }}</router-link>
          </h3>
          <p class="feed-url">{{ feed.url }}</p>
          <p class="feed-description">{{ feed.description }}</p>
        </div>
        <div class="feed-actions">
          <button @click="refreshFeed(feed.id)" class="btn btn-success">刷新</button>
          <button @click="deleteFeed(feed.id)" class="btn btn-danger">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { feedAPI } from '../api'

export default {
  name: 'FeedList',
  data() {
    return {
      feeds: [],
      loading: true,
      error: null,
      showAddForm: false,
      newFeedUrl: '',
      adding: false
    }
  },
  async mounted() {
    await this.loadFeeds()
  },
  methods: {
    async loadFeeds() {
      try {
        this.loading = true
        const response = await feedAPI.getFeeds()
        this.feeds = response.data
      } catch (err) {
        this.error = '加载订阅失败: ' + err.message
      } finally {
        this.loading = false
      }
    },
    async addFeed() {
      if (!this.newFeedUrl) return
      try {
        this.adding = true
        this.error = null
        await feedAPI.createFeed(this.newFeedUrl)
        this.newFeedUrl = ''
        this.showAddForm = false
        await this.loadFeeds()
      } catch (err) {
        this.error = '添加订阅失败: ' + err.message
      } finally {
        this.adding = false
      }
    },
    async deleteFeed(id) {
      if (!confirm('确定要删除这个订阅吗？')) return
      try {
        await feedAPI.deleteFeed(id)
        await this.loadFeeds()
      } catch (err) {
        this.error = '删除失败: ' + err.message
      }
    },
    async refreshFeed(id) {
      try {
        this.error = null
        await feedAPI.refreshFeed(id)
        alert('刷新成功！')
      } catch (err) {
        this.error = '刷新失败: ' + err.message
      }
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
}

.add-form {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.add-form input {
  flex: 1;
}

.feeds {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.feed-card {
  display: flex;
  justify-content: space-between;
  align-items: start;
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  transition: box-shadow 0.3s;
}

.feed-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.feed-info {
  flex: 1;
}

.feed-title {
  margin-bottom: 0.5rem;
}

.feed-title a {
  color: #2c3e50;
  text-decoration: none;
}

.feed-title a:hover {
  color: #3498db;
  text-decoration: underline;
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
}

.feed-actions {
  display: flex;
  gap: 0.5rem;
}
</style>
