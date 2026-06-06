package com.example.rssreader.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rssreader.ArticleDetailActivity;
import com.example.rssreader.R;
import com.example.rssreader.TranslationService;
import com.example.rssreader.database.ArticleEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private List<ArticleEntity> articles = new ArrayList<>();
    private Context context;
    private Map<Integer, String> translatedTitles = new HashMap<>();
    private Map<Integer, String> originalTitles = new HashMap<>();

    public ArticleAdapter(Context context) {
        this.context = context;
    }

    public void setArticles(List<ArticleEntity> articles) {
        this.articles = articles;
        translatedTitles.clear();
        originalTitles.clear();
        notifyDataSetChanged();
    }

    // 翻译可见的文章标题
    public void translateVisibleTitles() {
        for (int i = 0; i < articles.size(); i++) {
            ArticleEntity article = articles.get(i);
            if (!translatedTitles.containsKey(i)) {
                originalTitles.put(i, article.getTitle());
                translateTitle(i, article.getTitle());
            }
        }
    }

    // 恢复原文标题
    public void restoreOriginalTitles() {
        translatedTitles.clear();
        notifyDataSetChanged();
    }

    private void translateTitle(int position, String title) {
        TranslationService.translate(title, new TranslationService.TranslationCallback() {
            @Override
            public void onSuccess(String translatedText) {
                translatedTitles.put(position, translatedText);
                notifyItemChanged(position);
            }

            @Override
            public void onError(String error) {
                // 静默失败，不显示错误
            }
        });
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        ArticleEntity article = articles.get(position);
        holder.bind(article, position);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    class ArticleViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText;
        private TextView authorText;
        private TextView descriptionText;
        private TextView publishedText;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.articleTitle);
            authorText = itemView.findViewById(R.id.articleAuthor);
            descriptionText = itemView.findViewById(R.id.articleDescription);
            publishedText = itemView.findViewById(R.id.articlePublished);
        }

        public void bind(ArticleEntity article, int position) {
            // 显示翻译后的标题（如果有），否则显示原文
            if (translatedTitles.containsKey(position)) {
                titleText.setText(translatedTitles.get(position));
            } else {
                titleText.setText(article.getTitle());
            }

            if (article.getAuthor() != null && !article.getAuthor().isEmpty()) {
                authorText.setText("作者: " + article.getAuthor());
                authorText.setVisibility(View.VISIBLE);
            } else {
                authorText.setVisibility(View.GONE);
            }

            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                descriptionText.setText(article.getDescription());
                descriptionText.setVisibility(View.VISIBLE);
            } else {
                descriptionText.setVisibility(View.GONE);
            }

            if (article.getPublished() != null && !article.getPublished().isEmpty()) {
                publishedText.setText(formatDate(article.getPublished()));
                publishedText.setVisibility(View.VISIBLE);
            } else {
                publishedText.setVisibility(View.GONE);
            }

            // 点击文章打开详情页
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ArticleDetailActivity.class);
                intent.putExtra("title", article.getTitle());
                intent.putExtra("author", article.getAuthor());
                intent.putExtra("published", article.getPublished());
                intent.putExtra("content", article.getContent() != null ? article.getContent() : article.getDescription());
                intent.putExtra("link", article.getLink());
                intent.putExtra("articleId", article.getId());
                intent.putExtra("isFavorited", article.isFavorited());
                context.startActivity(intent);
            });
        }

        private String formatDate(String dateString) {
            if (dateString == null) return "";
            return dateString.substring(0, Math.min(dateString.length(), 19)).replace("T", " ");
        }
    }
}
