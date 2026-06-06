# 前端 README

这是 RSS 阅读器项目的 Web 前端，使用 Vue 3 + Vite 开发。

## 启动方式

进入前端目录并安装依赖：

```bash
cd frontend
npm install
```

启动开发服务：

```bash
npm run dev
```

前端访问地址：

```text
http://localhost:5173
```

也可以在项目根目录使用启动脚本：

```bash
./start_frontend.sh
```

## 构建

```bash
npm run build
```

预览构建产物：

```bash
npm run preview
```

## 后端代理

`vite.config.js` 会把前端的 `/api/*` 请求代理到：

```text
http://localhost:8000
```

使用 Web 前端前，请先启动后端，尤其是需要订阅、文章、收藏、翻译或 AI 总结功能时。

## 代码结构

- `src/main.js`：Vue 应用入口，挂载路由。
- `src/App.vue`：应用外壳。
- `src/api.js`：Axios 接口封装。
- `src/components/ArticleList.vue`：文章列表，支持最新文章、收藏文章、收藏操作、翻译和 AI 总结。
- `src/components/FeedList.vue`：RSS 订阅管理。
- `src/components/FeedDetail.vue`：单个订阅源的文章详情列表。

当前前端状态主要保存在组件内部，没有使用 Vuex/Pinia 等全局状态管理。

## 主要接口

前端通过 `src/api.js` 调用后端接口：

- `GET /feeds/`：获取订阅列表。
- `POST /feeds/`：添加订阅。
- `DELETE /feeds/{id}`：删除订阅。
- `POST /feeds/{id}/refresh`：刷新订阅。
- `GET /articles/`：获取文章列表。
- `POST /articles/{id}/favorite`：切换收藏状态。
- `GET /articles/favorites/`：获取收藏文章。
- `POST /translate/`：翻译文本。
- `POST /summarize/`：AI 总结。

## 注意事项

AI 总结依赖后端 AI 配置。如果后端没有配置可用的 AI API Key，前端点击 AI 总结会失败。