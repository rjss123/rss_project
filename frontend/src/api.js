import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const feedAPI = {
  getFeeds() {
    return api.get('/feeds/')
  },
  getFeed(id) {
    return api.get(`/feeds/${id}`)
  },
  createFeed(url) {
    return api.post('/feeds/', { url })
  },
  deleteFeed(id) {
    return api.delete(`/feeds/${id}`)
  },
  refreshFeed(id) {
    return api.post(`/feeds/${id}/refresh`)
  },
  getArticles(limit = 50) {
    return api.get('/articles/', { params: { limit } })
  },
  toggleFavorite(id) {
    return api.post(`/articles/${id}/favorite`)
  },
  getFavorites() {
    return api.get('/articles/favorites/')
  },
  translate(text) {
    return api.post('/translate/', null, { params: { text } })
  },
  summarize({ text, url }) {
    return api.post('/summarize/', null, { params: { text, url } })
  }
}

export default api
