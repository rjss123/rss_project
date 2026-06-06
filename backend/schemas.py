from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List

class FeedBase(BaseModel):
    title: str
    url: str
    description: Optional[str] = None

class FeedCreate(BaseModel):
    url: str

class Feed(FeedBase):
    id: int
    created_at: str
    updated_at: str

class ArticleBase(BaseModel):
    title: str
    link: str
    description: Optional[str] = None
    published: Optional[str] = None
    author: Optional[str] = None
    content: Optional[str] = None

class Article(ArticleBase):
    id: int
    feed_id: int
    is_favorited: bool = False
    created_at: str

class FeedWithArticles(Feed):
    articles: List[Article] = []
