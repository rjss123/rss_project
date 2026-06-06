# 后端 README

这是 RSS 阅读器项目的 FastAPI 后端，提供 RSS 订阅接口、文章接口、收藏持久化、翻译、网页正文提取和 AI 总结能力。

## 启动方式

在项目根目录创建并激活 Conda 环境：

```bash
conda env create -f environment.yml
conda activate rss_project
```

启动后端：

```bash
cd backend
python main.py
```

服务地址：

```text
http://localhost:8000
```

接口文档：

```text
http://localhost:8000/docs
```

也可以在项目根目录使用启动脚本：

```bash
./start_backend.sh
```

## 依赖说明

主要依赖在根目录的 `environment.yml` 中声明：

- FastAPI
- Uvicorn
- feedparser
- httpx
- SQLAlchemy + aiosqlite
- pydantic
- python-dotenv

网页正文提取使用 BeautifulSoup。如果环境创建较早，缺少依赖时需要额外安装：

```bash
pip install beautifulsoup4 lxml
```

## 代码结构

- `main.py`：FastAPI 入口，定义所有接口。
- `services.py`：RSS 解析、订阅缓存、文章缓存、收藏逻辑。
- `translator.py`：翻译前的文本清理和翻译调用。
- `database.py`：SQLite 数据库配置。
- `models.py`：收藏文章的数据模型。
- `schemas.py`：接口请求和响应结构。

## 数据存储策略

当前后端采用混合存储：

- 普通订阅源：内存缓存。
- 普通文章：内存缓存。
- 收藏文章：SQLite 持久化。

收藏数据库位置：

```text
backend/data/favorites.db
```

因此，后端重启后普通订阅和普通文章会清空，但收藏文章会保留。

## 主要接口

RSS 和文章接口：

- `POST /feeds/`：添加订阅。
- `GET /feeds/`：获取订阅列表。
- `GET /feeds/{feed_id}`：获取单个订阅和文章。
- `DELETE /feeds/{feed_id}`：删除订阅。
- `POST /feeds/{feed_id}/refresh`：刷新订阅。
- `GET /articles/`：获取文章列表。
- `POST /articles/{article_id}/favorite`：切换收藏状态。
- `GET /articles/favorites/`：获取收藏文章。

翻译和 AI 接口：

- `POST /translate/`：文本翻译。
- `POST /extract-content/`：从网页 URL 提取正文。
- `POST /summarize/`：AI 总结。
- `GET /ai-config/`：获取 AI 配置。
- `POST /ai-config/`：更新 AI 配置。

## 与 Android 的关系

Android 应用当前可以在本地直接解析 RSS，不依赖后端完成普通订阅阅读。后端主要用于 Web 前端，以及 Android 可选的 AI 总结和翻译能力。