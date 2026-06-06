# Android README

这是 RSS 阅读器项目的 Android 客户端，使用 Java + XML 布局开发。当前 Android 端是独立 RSS 阅读器：普通订阅、RSS 解析、文章缓存、收藏和阅读都在 App 内完成，不依赖 FastAPI 后端；AI 总结和部分翻译能力可以通过配置外部 AI API 使用。

## 开发环境

- Android Studio
- JDK 21
- Android SDK 36
- Gradle / Android Gradle Plugin 见 `build.gradle.kts`

## 构建命令

在 `android/` 目录下执行：

```bash
./gradlew assembleDebug
```

构建 Release：

```bash
./gradlew assembleRelease
```

运行测试：

```bash
./gradlew test
./gradlew connectedAndroidTest
```

如果 Gradle Wrapper 报 `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`，需要通过 Android Studio 或本机 Gradle 恢复/重新生成 wrapper 文件。

## 当前技术栈

- Java 21
- XML 布局
- AppCompat + Material Components
- ViewPager2 + BottomNavigationView
- RecyclerView
- Room 本地数据库
- OkHttp 获取 RSS XML
- Jsoup 网页正文提取
- WebView 显示文章正文

## 应用结构

### 主导航

`MainActivity` 承载底部三 Tab：

- 首页：`HomeFragment`
- 收藏：`FavoritesFragment`
- 订阅：`FeedsFragment`

底部导航菜单定义在：

```text
app/src/main/res/menu/bottom_nav_menu.xml
```

主布局：

```text
app/src/main/res/layout/activity_main.xml
```

### 首页

`HomeFragment` 负责：

- 从本地 Room 数据库加载文章。
- 搜索文章。
- 下拉刷新。
- 右下角全局翻译按钮：一键翻译文章列表标题，再次点击恢复原文。

文章列表适配器：

```text
app/src/main/java/com/example/rssreader/adapter/ArticleAdapter.java
```

### 收藏页

`FavoritesFragment` 负责展示本地数据库中已收藏的文章。

收藏状态在文章详情页切换，并通过 `RssRepository.toggleFavorite()` 更新 Room 数据库。

### 订阅页

`FeedsFragment` 负责 RSS 订阅管理：

- 添加 RSS URL。
- 展示订阅列表。
- 刷新订阅。
- 删除订阅。
- 进入 AI 配置页面。

订阅列表适配器：

```text
app/src/main/java/com/example/rssreader/adapter/FeedAdapter.java
```

## 数据流

Android 端普通阅读流程：

```text
用户添加 RSS URL
    ↓
RssRepository 使用 OkHttp 获取 RSS XML
    ↓
RssParser 解析 RSS / Atom
    ↓
ArticleEntity / FeedEntity 写入 Room
    ↓
HomeFragment / FavoritesFragment 从 Room 读取并展示
```

核心类：

- `RssRepository.java`：数据仓库，负责 RSS 获取、解析、去重、数据库读写。
- `RssParser.java`：RSS/Atom 解析器。
- `database/AppDatabase.java`：Room 数据库。
- `database/ArticleEntity.java`：文章实体。
- `database/FeedEntity.java`：订阅实体。
- `database/ArticleDao.java`：文章 DAO。
- `database/FeedDao.java`：订阅 DAO。

## RSS 解析说明

`RssParser` 会尝试解析：

- RSS 2.0 的 `item`
- Atom 的 `entry`
- `title`
- `link`
- `description`
- `summary`
- `content`
- `content:encoded`
- `dc:creator`
- `pubDate` / `published` / `updated`

正文优先级大致为：

1. `content:encoded`
2. `content`
3. 包含 HTML 的 `description`
4. 普通 `description` / `summary`
5. 没有正文时显示无正文提示

注意：部分 RSS 源本身不提供正文，例如 Hacker News RSS 通常只提供标题、原文链接、评论链接、积分和评论数。这种情况下 App 无法通过 RSS 直接得到正文，只能打开原网页或使用 AI/网页提取辅助总结。

## 文章详情页

`ArticleDetailActivity` 负责：

- 显示完整标题。
- 显示作者和发布时间。
- 用 WebView 展示正文 HTML。
- 全文翻译。
- AI 总结。
- 收藏/取消收藏。
- 右下角按钮打开原网页。

布局文件：

```text
app/src/main/res/layout/activity_article_detail.xml
```

## AI 总结

AI 总结相关类：

- `AIConfigActivity.java`：AI 配置页面。
- `AIConfigManager.java`：使用 SharedPreferences 保存 AI 配置。
- `AISummaryService.java`：调用 AI API 生成总结。
- `WebContentExtractor.java`：使用 Jsoup 从网页提取正文。

配置入口在订阅页。

当前 AI 配置字段：

- API 地址，例如：`https://api.deepseek.com`
- API Key
- 模型名，例如：`deepseek-v4-flash`
- Provider，例如：`deepseek`

API Key 不应硬编码进 APK。用户需要在 AI 配置页面输入并保存。

## 翻译

翻译相关类：

```text
TranslationService.java
```

当前首页翻译是全局按钮模式：点击后把列表中的文章标题替换为译文，再次点击恢复原文。

文章详情页提供全文翻译按钮，译文显示在详情页卡片中。

## Release 打包

Release 配置在：

```text
app/build.gradle.kts
app/proguard-rules.pro
```

Release 当前启用：

- `isMinifyEnabled = true`
- `isShrinkResources = true`
- ProGuard/R8 混淆和资源压缩

打包：

```bash
./gradlew assembleRelease
```

如果 R8 报 missing classes，查看：

```text
app/build/outputs/mapping/release/missing_rules.txt
```

并把需要的 `-dontwarn` 或 keep 规则补到 `proguard-rules.pro`。

## 常见问题

### Release APK 打不开

优先查看 Logcat：

```bash
adb logcat -c
adb logcat | grep com.example.rssreader
```

重点查看 `FATAL EXCEPTION` 后面的堆栈。

常见原因包括：

- R8 混淆裁掉 Room 生成类。
- 资源 ID 或布局更新后未 clean。
- 旧 APK/旧数据库残留。

可以先尝试：

```bash
./gradlew clean
./gradlew assembleRelease
```

必要时卸载旧 App 后重装。

### 文章重复

文章去重依赖链接。修改 RSS 解析或数据库逻辑时，应保持以文章 link 作为主要去重依据。

### 某些 RSS 没有正文

这是 RSS 源本身限制，不一定是解析错误。Hacker News RSS 就经常只提供标题和链接。