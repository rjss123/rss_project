from googletrans import Translator
import asyncio
from typing import Optional
import re
import html

translator = Translator()

def clean_html_for_translation(text: str) -> str:
    """清理HTML标签和实体，但保留可翻译的文本"""
    # 解码HTML实体 (如 &#39; -> ')
    text = html.unescape(text)

    # 移除HTML标签
    tag_pattern = re.compile(r'<[^>]+>')
    text = tag_pattern.sub(' ', text)

    # 移除URL链接
    url_pattern = re.compile(r'https?://[^\s]+')
    text = url_pattern.sub('', text)

    # 移除代码块 `code`
    code_pattern = re.compile(r'`[^`]+`')
    text = code_pattern.sub('', text)

    # 移除 # 符号（但保留后面的文本）
    text = re.sub(r'#\s*', '', text)

    # 移除分隔符 |
    text = re.sub(r'\s*\|\s*', ' ', text)

    # 移除多余的空格
    text = re.sub(r'\s+', ' ', text).strip()

    return text

def should_translate(text: str) -> bool:
    """判断文本是否需要翻译"""
    if not text or len(text.strip()) < 2:
        return False

    # 只包含数字、标点和空格，不翻译
    if re.match(r'^[\d\s\W]+$', text):
        return False

    # 检查是否主要是中文
    chinese_chars = len(re.findall(r'[一-鿿]', text))
    if chinese_chars > len(text) * 0.3:  # 超过30%是中文
        return False

    # 检查是否包含至少一个英文单词（2个字母以上）
    if not re.search(r'[a-zA-Z]{2,}', text):
        return False

    return True

async def translate_text(text: str, dest: str = 'zh-cn') -> Optional[str]:
    """翻译文本到指定语言"""
    try:
        # 清理HTML标签、# 符号等
        cleaned_text = clean_html_for_translation(text)

        # 判断是否需要翻译
        if not should_translate(cleaned_text):
            return text  # 返回原文

        # 执行翻译
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(
            None,
            lambda: translator.translate(cleaned_text, dest=dest)
        )

        translated = result.text

        # 如果翻译结果为空，返回原文
        if not translated or not translated.strip():
            return text

        return translated

    except Exception as e:
        print(f"Translation error: {e}")
        return text  # 出错时返回原文

async def detect_language(text: str) -> Optional[str]:
    """检测文本语言"""
    try:
        cleaned_text = clean_html_for_translation(text)

        if not cleaned_text or len(cleaned_text.strip()) < 2:
            return None

        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(
            None,
            lambda: translator.detect(cleaned_text)
        )
        return result.lang
    except Exception as e:
        print(f"Language detection error: {e}")
        return None
