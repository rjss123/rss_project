from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncSession
from typing import List
import uvicorn
import httpx
from bs4 import BeautifulSoup

from database import get_db, engine, Base
import schemas
import services
from translator import translate_text

app = FastAPI(title="RSS Reader API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# AI API 配置（全局变量，可通过 API 修改）
ai_config = {
    "provider": "openai",  # openai, claude, gemini
    "api_key": "",
    "api_url": "https://api.openai.com/v1/chat/completions",
    "model": "gpt-3.5-turbo"
}

@app.on_event("startup")
async def startup():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

@app.get("/")
async def root():
    return {"message": "RSS Reader API", "docs": "/docs"}

# ========== AI 配置管理 ==========

@app.get("/ai-config/")
async def get_ai_config():
    """获取 AI 配置"""
    # 隐藏 API Key
    safe_config = ai_config.copy()
    if safe_config["api_key"]:
        safe_config["api_key"] = safe_config["api_key"][:8] + "****"
    return safe_config

@app.post("/ai-config/")
async def update_ai_config(
    provider: str = None,
    api_key: str = None,
    api_url: str = None,
    model: str = None
):
    """更新 AI 配置"""
    if provider:
        ai_config["provider"] = provider
    if api_key:
        ai_config["api_key"] = api_key
    if api_url:
        ai_config["api_url"] = api_url
    if model:
        ai_config["model"] = model

    return {"message": "AI 配置已更新", "config": await get_ai_config()}

# ========== 网页抓取和 AI 总结 ==========

@app.post("/extract-content/")
async def extract_content(url: str):
    """从 URL 提取网页正文"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(url, headers={
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            })
            response.raise_for_status()

            # 使用 BeautifulSoup 提取正文
            soup = BeautifulSoup(response.text, 'html.parser')

            # 移除无用元素
            for tag in soup(['script', 'style', 'nav', 'header', 'footer', 'aside']):
                tag.decompose()

            # 尝试找到主要内容
            main_content = (
                soup.find('article') or
                soup.find('main') or
                soup.find(class_=['post-content', 'article-content', 'entry-content']) or
                soup.find(id=['content', 'main'])
            )

            if main_content:
                text = main_content.get_text(separator='\n', strip=True)
            else:
                # 降级：提取所有段落
                paragraphs = soup.find_all('p')
                text = '\n\n'.join([p.get_text(strip=True) for p in paragraphs if len(p.get_text(strip=True)) > 50])

            # 限制长度
            if len(text) > 5000:
                text = text[:5000] + "..."

            return {"content": text, "length": len(text)}

    except Exception as e:
        raise HTTPException(status_code=400, detail=f"抓取失败: {str(e)}")

@app.post("/summarize/")
async def summarize_article(text: str = None, url: str = None):
    """AI 总结文章

    参数:
        text: 直接提供文本内容
        url: 提供 URL，自动抓取内容
    """
    try:
        # 如果提供 URL，先抓取内容
        if url and not text:
            extract_result = await extract_content(url)
            text = extract_result["content"]

        if not text:
            raise HTTPException(status_code=400, detail="必须提供 text 或 url")

        # 检查 AI 配置
        if not ai_config["api_key"]:
            raise HTTPException(status_code=400, detail="未配置 AI API Key")

        # 调用 AI API 生成总结
        summary = await generate_ai_summary(text)

        return {
            "summary": summary,
            "original_length": len(text),
            "provider": ai_config["provider"]
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"总结失败: {str(e)}")

async def generate_ai_summary(text: str) -> str:
    """调用 AI API 生成总结"""

    # 限制文本长度
    if len(text) > 3000:
        text = text[:3000] + "..."

    provider = ai_config["provider"]

    if provider == "openai":
        return await summarize_with_openai(text)
    elif provider == "claude":
        return await summarize_with_claude(text)
    else:
        raise Exception(f"不支持的 AI 提供商: {provider}")

async def summarize_with_openai(text: str) -> str:
    """使用 OpenAI API 总结"""
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.post(
            ai_config["api_url"],
            headers={
                "Authorization": f"Bearer {ai_config['api_key']}",
                "Content-Type": "application/json"
            },
            json={
                "model": ai_config["model"],
                "messages": [
                    {
                        "role": "system",
                        "content": "你是一个专业的文章总结助手。请用中文总结文章的核心内容，保持简洁明了，控制在 200 字以内。"
                    },
                    {
                        "role": "user",
                        "content": f"请总结以下文章：\n\n{text}"
                    }
                ],
                "max_tokens": 300,
                "temperature": 0.7
            }
        )

        if response.status_code != 200:
            raise Exception(f"OpenAI API 错误: {response.status_code} - {response.text}")

        result = response.json()
        summary = result["choices"][0]["message"]["content"]
        return summary.strip()

async def summarize_with_claude(text: str) -> str:
    """使用 Claude API 总结"""
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.post(
            "https://api.anthropic.com/v1/messages",
            headers={
                "x-api-key": ai_config["api_key"],
                "anthropic-version": "2023-06-01",
                "Content-Type": "application/json"
            },
            json={
                "model": ai_config.get("model", "claude-3-haiku-20240307"),
                "max_tokens": 300,
                "messages": [
                    {
                        "role": "user",
                        "content": f"请用中文总结以下文章的核心内容，保持简洁明了，控制在 200 字以内：\n\n{text}"
                    }
                ]
            }
        )

        if response.status_code != 200:
            raise Exception(f"Claude API 错误: {response.status_code} - {response.text}")

        result = response.json()
        summary = result["content"][0]["text"]
        return summary.strip()

# ========== RSS 订阅管理 ==========

@app.post("/feeds/", response_model=schemas.Feed)
async def create_feed(feed: schemas.FeedCreate, db: AsyncSession = Depends(get_db)):
    """创建新的 RSS 订阅"""
    try:
        new_feed = await services.RSSService.create_feed(db, feed.url)
        return schemas.Feed(**new_feed)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to add feed: {str(e)}")

@app.get("/feeds/", response_model=List[schemas.Feed])
async def get_feeds(db: AsyncSession = Depends(get_db)):
    """获取所有订阅"""
    feeds = await services.RSSService.get_feeds(db)
    return [schemas.Feed(**f) for f in feeds]

@app.get("/feeds/{feed_id}", response_model=schemas.FeedWithArticles)
async def get_feed(feed_id: int, db: AsyncSession = Depends(get_db)):
    """获取单个订阅及其文章"""
    feed = await services.RSSService.get_feed(db, feed_id)
    if not feed:
        raise HTTPException(status_code=404, detail="Feed not found")
    return schemas.FeedWithArticles(**feed)

@app.delete("/feeds/{feed_id}")
async def delete_feed(feed_id: int, db: AsyncSession = Depends(get_db)):
    """删除订阅"""
    success = await services.RSSService.delete_feed(db, feed_id)
    if not success:
        raise HTTPException(status_code=404, detail="Feed not found")
    return {"message": "Feed deleted successfully"}

@app.post("/feeds/{feed_id}/refresh")
async def refresh_feed(feed_id: int, db: AsyncSession = Depends(get_db)):
    """刷新订阅内容"""
    try:
        await services.RSSService.refresh_feed(db, feed_id)
        return {"message": "Feed refreshed successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to refresh feed: {str(e)}")

@app.get("/articles/", response_model=List[schemas.Article])
async def get_articles(limit: int = 50, db: AsyncSession = Depends(get_db)):
    """获取最新文章"""
    articles = await services.RSSService.get_articles(db, limit)
    return articles

@app.post("/articles/{article_id}/favorite")
async def toggle_favorite(article_id: int, db: AsyncSession = Depends(get_db)):
    """切换文章收藏状态"""
    try:
        article = await services.RSSService.toggle_favorite(db, article_id)
        if not article:
            raise HTTPException(status_code=404, detail="Article not found")
        return {"message": "Favorite toggled", "is_favorited": article["is_favorited"]}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to toggle favorite: {str(e)}")

@app.get("/articles/favorites/", response_model=List[schemas.Article])
async def get_favorites(db: AsyncSession = Depends(get_db)):
    """获取收藏的文章"""
    articles = await services.RSSService.get_favorites(db)
    return articles

@app.post("/translate/")
async def translate(text: str):
    """翻译文本到中文"""
    try:
        translated = await translate_text(text, dest='zh-cn')
        if translated:
            return {"original": text, "translated": translated}
        else:
            raise HTTPException(status_code=500, detail="Translation failed")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Translation error: {str(e)}")

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
