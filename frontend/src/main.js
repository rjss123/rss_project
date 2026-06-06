import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import FeedList from './components/FeedList.vue'
import ArticleList from './components/ArticleList.vue'
import FeedDetail from './components/FeedDetail.vue'

const routes = [
  { path: '/', component: ArticleList },
  { path: '/feeds', component: FeedList },
  { path: '/feeds/:id', component: FeedDetail }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

createApp(App).use(router).mount('#app')
