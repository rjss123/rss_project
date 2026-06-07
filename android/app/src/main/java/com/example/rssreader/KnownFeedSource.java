package com.example.rssreader;

import java.util.Arrays;
import java.util.List;

public class KnownFeedSource {
    private final String category;
    private final String title;
    private final String url;
    private final String aliases;

    public KnownFeedSource(String category, String title, String url, String aliases) {
        this.category = category;
        this.title = title;
        this.url = url;
        this.aliases = aliases;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getAliases() {
        return aliases;
    }

    public String getDisplayName() {
        return category + " - " + title;
    }

    public static List<KnownFeedSource> getAll() {
        return Arrays.asList(
                // ── 科技 · 中文（高频更新）──
                new KnownFeedSource("科技", "36氪", "https://www.36kr.com/feed", "36kr.com 36氪 36kr"),
                new KnownFeedSource("科技", "IT之家", "https://www.ithome.com/rss/", "ithome.com ithome it之家"),
                new KnownFeedSource("科技", "少数派", "https://sspai.com/feed", "sspai.com 少数派"),
                new KnownFeedSource("科技", "Solidot 奇客", "https://www.solidot.org/index.rss", "solidot.org solidot"),
                new KnownFeedSource("科技", "Linux.do", "https://linux.do/latest.rss", "linux.do"),
                new KnownFeedSource("科技", "机器之心", "https://www.jiqizhixin.com/rss", "jiqizhixin.com 机器之心 jiqizhixin"),
                new KnownFeedSource("科技", "InfoQ 中文", "https://www.infoq.cn/feed", "infoq.cn infoq"),

                // ── 科技 · 英文（高频更新）──
                new KnownFeedSource("科技", "Hacker News", "https://hnrss.org/frontpage", "hnrss.org hacker news hn"),
                new KnownFeedSource("科技", "TechCrunch", "https://techcrunch.com/feed/", "techcrunch.com tech crunch"),
                new KnownFeedSource("科技", "The Verge", "https://www.theverge.com/rss/index.xml", "theverge.com verge"),
                new KnownFeedSource("科技", "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", "arstechnica.com ars"),
                new KnownFeedSource("科技", "Wired", "https://www.wired.com/feed/rss", "wired.com wired"),
                new KnownFeedSource("科技", "The Register", "https://www.theregister.com/headlines.atom", "theregister.com register"),

                // ── AI / 研究 ──
                new KnownFeedSource("科技", "OpenAI Blog", "https://openai.com/news/rss.xml", "openai.com openai"),
                new KnownFeedSource("科技", "DeepMind Blog", "https://deepmind.google/blog/rss.xml", "deepmind.google deepmind"),
                new KnownFeedSource("科技", "Hugging Face Blog", "https://huggingface.co/blog/feed.xml", "huggingface.co hugging face"),
                new KnownFeedSource("科技", "GitHub Blog", "https://github.blog/feed/", "github.blog github"),
                new KnownFeedSource("科技", "AWS News Blog", "https://aws.amazon.com/blogs/aws/feed/", "aws.amazon.com aws"),

                // ── 生活 · 新闻 ──
                new KnownFeedSource("生活", "BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml", "bbc.co.uk bbc"),
                new KnownFeedSource("生活", "Reuters Top News", "https://www.reutersagency.com/feed/", "reuters.com reuters 路透"),
                new KnownFeedSource("生活", "NPR News", "https://feeds.npr.org/1001/rss.xml", "npr.org npr"),

                // ── 生活 · 阅读 ──
                new KnownFeedSource("生活", "阮一峰的网络日志", "http://www.ruanyifeng.com/blog/atom.xml", "ruanyifeng.com 阮一峰"),
                new KnownFeedSource("生活", "豆瓣最受欢迎书评", "https://www.douban.com/feed/review/book", "douban.com 豆瓣 书评"),
                new KnownFeedSource("生活", "知乎每日精选", "https://www.zhihu.com/rss", "zhihu.com 知乎"),

                // ── 生活 · 健康 / 旅行 ──
                new KnownFeedSource("生活", "Lifehacker", "https://lifehacker.com/feed/rss", "lifehacker.com lifehacker")
        );
    }

    /** 已失效或不再更新的源，自动清理 */
    public static List<String> getRemovedUrls() {
        return Arrays.asList(
                // 旧 RSSHub 路径（已迁移或失效）
                "https://rsshub.app/weibo/search/hot",
                "https://rsshub.app/zhihu/hot",
                "https://rsshub.app/bilibili/ranking/0/3/1",
                "https://rsshub.app/github/trending/daily",
                "https://rsshub.app/juejin/trending/all/weekly",
                "https://rsshub.app/people/rmrb",
                "https://rsshub.app/people/news",
                "https://rsshub.app/people/politics",
                "https://rsshub.app/people/world",
                // 失效或停更的源
                "https://rss.arxiv.org/rss/cs.AI",
                "https://www.v2ex.com/feed/tab/hot.xml",
                "https://wallstreetcn.com/rss",
                "https://cn.reuters.com/tools/rss/",
                "http://cn.reuters.com/tools/rss/",
                "https://www.xinhuanet.com/rss.htm",
                "http://www.xinhuanet.com/rss.htm",
                "https://www.zaobao.com/rss/rss",
                "http://www.zaobao.com/rss/rss",
                "https://netflixtechblog.com/feed",
                "https://feed.iplaysoft.com",
                "http://feed.iplaysoft.com",
                "https://feeds.appinn.com/appinns/",
                "http://feeds.appinn.com/appinns/",
                "https://www.portablesoft.org/feed/",
                "http://www.portablesoft.org/feed/",
                "https://www.nationalgeographic.com.cn/index.php?m=content&c=feed",
                "http://www.nationalgeographic.com.cn/index.php?m=content&c=feed",
                "https://songshuhui.net/feed",
                "http://songshuhui.net/feed",
                // 旧的 Bon Appétit（已移除，非科技/新闻类）
                "https://www.bonappetit.com/feed/recipes-rss-feed/rss"
        );
    }
}
