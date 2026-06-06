import feedparser
import httpx
from datetime import datetime
from typing import List, Optional, Dict
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
import models

# 内存缓存：存储订阅和文章
feeds_cache: Dict[int, dict] = {}
articles_cache: Dict[int, dict] = {}
next_feed_id = 1
next_article_id = 1

class RSSService:
    @staticmethod
    async def parse_feed(url: str) -> dict:
        """解析 RSS 源"""
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(url)
            response.raise_for_status()
            feed_data = feedparser.parse(response.text)
            return feed_data

    @staticmethod
    async def create_feed(db: AsyncSession, url: str) -> dict:
        """创建新的 RSS 订阅（内存存储）"""
        global next_feed_id

        feed_data = await RSSService.parse_feed(url)

        feed = {
            "id": next_feed_id,
            "title": feed_data.feed.get("title", "Unknown Feed"),
            "url": url,
            "description": feed_data.feed.get("description", ""),
            "created_at": datetime.utcnow().isoformat(),
            "updated_at": datetime.utcnow().isoformat()
        }

        feeds_cache[next_feed_id] = feed
        next_feed_id += 1

        await RSSService.fetch_articles(db, feed, feed_data)

        return feed

    @staticmethod
    async def fetch_articles(db: AsyncSession, feed: dict, feed_data: dict = None):
        """获取 RSS 源的文章（内存存储）"""
        global next_article_id

        if feed_data is None:
            feed_data = await RSSService.parse_feed(feed["url"])

        for entry in feed_data.entries:
            link = entry.get("link", "")

            # 检查是否已存在
            if any(a["link"] == link for a in articles_cache.values()):
                continue

            published = None
            if hasattr(entry, "published_parsed") and entry.published_parsed:
                try:
                    published = datetime(*entry.published_parsed[:6]).isoformat()
                except:
                    pass

            # 检查是否被收藏
            result = await db.execute(
                select(models.FavoriteArticle).where(models.FavoriteArticle.link == link)
            )
            is_favorited = result.scalar_one_or_none() is not None

            article = {
                "id": next_article_id,
                "feed_id": feed["id"],
                "title": entry.get("title", "No Title"),
                "link": link,
                "description": entry.get("summary", ""),
                "published": published,
                "author": entry.get("author", None),
                "content": entry.get("content", [{}])[0].get("value", "") if entry.get("content") else entry.get("summary", ""),
                "is_favorited": is_favorited,
                "created_at": datetime.utcnow().isoformat()
            }

            articles_cache[next_article_id] = article
            next_article_id += 1

    @staticmethod
    async def get_feeds(db: AsyncSession) -> List[dict]:
        """获取所有订阅"""
        return list(feeds_cache.values())

    @staticmethod
    async def get_feed(db: AsyncSession, feed_id: int) -> Optional[dict]:
        """获取单个订阅"""
        feed = feeds_cache.get(feed_id)
        if feed:
            feed_articles = [a for a in articles_cache.values() if a["feed_id"] == feed_id]
            return {**feed, "articles": feed_articles}
        return None

    @staticmethod
    async def delete_feed(db: AsyncSession, feed_id: int) -> bool:
        """删除订阅"""
        if feed_id in feeds_cache:
            del feeds_cache[feed_id]
            # 删除相关文章
            to_delete = [aid for aid, a in articles_cache.items() if a["feed_id"] == feed_id]
            for aid in to_delete:
                del articles_cache[aid]
            return True
        return False

    @staticmethod
    async def get_articles(db: AsyncSession, limit: int = 50) -> List[dict]:
        """获取最新文章"""
        sorted_articles = sorted(
            articles_cache.values(),
            key=lambda x: x.get("published") or x.get("created_at", ""),
            reverse=True
        )
        return sorted_articles[:limit]

    @staticmethod
    async def refresh_feed(db: AsyncSession, feed_id: int):
        """刷新订阅内容"""
        feed = feeds_cache.get(feed_id)
        if feed:
            await RSSService.fetch_articles(db, feed)

    @staticmethod
    async def toggle_favorite(db: AsyncSession, article_id: int) -> Optional[dict]:
        """切换文章收藏状态"""
        article = articles_cache.get(article_id)
        if not article:
            return None

        is_favorited = article.get("is_favorited", False)

        if is_favorited:
            # 取消收藏 - 从数据库删除
            result = await db.execute(
                select(models.FavoriteArticle).where(models.FavoriteArticle.article_id == article_id)
            )
            fav = result.scalar_one_or_none()
            if fav:
                await db.delete(fav)
                await db.commit()
            article["is_favorited"] = False
        else:
            # 添加收藏 - 保存到数据库
            published = None
            if article.get("published"):
                try:
                    published = datetime.fromisoformat(article["published"])
                except:
                    pass

            fav = models.FavoriteArticle(
                article_id=article_id,
                feed_id=article["feed_id"],
                title=article["title"],
                link=article["link"],
                description=article.get("description"),
                published=published,
                author=article.get("author"),
                content=article.get("content")
            )
            db.add(fav)
            await db.commit()
            article["is_favorited"] = True

        return article

    @staticmethod
    async def get_favorites(db: AsyncSession) -> List[dict]:
        """获取收藏的文章"""
        result = await db.execute(
            select(models.FavoriteArticle).order_by(models.FavoriteArticle.favorited_at.desc())
        )
        favorites = result.scalars().all()

        # 转换为字典格式
        return [
            {
                "id": f.article_id,
                "feed_id": f.feed_id,
                "title": f.title,
                "link": f.link,
                "description": f.description,
                "published": f.published.isoformat() if f.published else None,
                "author": f.author,
                "content": f.content,
                "is_favorited": True,
                "created_at": f.created_at.isoformat()
            }
            for f in favorites
        ]
