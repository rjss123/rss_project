from sqlalchemy import Column, Integer, String, DateTime, Text, Boolean
from datetime import datetime
from database import Base

# 只保存收藏的文章到数据库
class FavoriteArticle(Base):
    __tablename__ = "favorite_articles"

    id = Column(Integer, primary_key=True, index=True)
    article_id = Column(Integer, unique=True, index=True)  # 原始文章 ID
    feed_id = Column(Integer)
    title = Column(String, index=True)
    link = Column(String, unique=True)
    description = Column(Text, nullable=True)
    published = Column(DateTime, nullable=True)
    author = Column(String, nullable=True)
    content = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    favorited_at = Column(DateTime, default=datetime.utcnow)
