package com.example.rssreader.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.rssreader.R;
import com.example.rssreader.RssRepository;
import com.example.rssreader.adapter.ArticleAdapter;
import com.example.rssreader.database.ArticleEntity;
import com.example.rssreader.database.FeedEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView errorText;
    private SearchView searchView;
    private FloatingActionButton fabTranslate;
    private RssRepository repository;
    private boolean isTranslated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new RssRepository(requireContext());

        initViews(view);
        setupRecyclerView();
        setupSearchView();
        setupTranslateButton();
        loadArticles();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        errorText = view.findViewById(R.id.errorText);
        searchView = view.findViewById(R.id.searchView);
        fabTranslate = view.findViewById(R.id.fabTranslate);

        swipeRefresh.setOnRefreshListener(this::loadArticles);
    }

    private void setupRecyclerView() {
        adapter = new ArticleAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchArticles(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadArticles();
                } else {
                    searchArticles(newText);
                }
                return true;
            }
        });
    }

    private void setupTranslateButton() {
        fabTranslate.setOnClickListener(v -> {
            if (isTranslated) {
                adapter.restoreOriginalTitles();
                isTranslated = false;
            } else {
                adapter.translateVisibleTitles();
                isTranslated = true;
            }
        });
    }

    private void loadArticles() {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);

        repository.getAllFeeds(new RssRepository.OnFeedsLoadedListener() {
            @Override
            public void onSuccess(List<FeedEntity> feeds) {
                if (feeds.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        errorText.setText("还没有订阅源\n\n点击右侧 [订阅] 标签页添加");
                        errorText.setVisibility(View.VISIBLE);
                        adapter.setArticles(new ArrayList<>());
                    });
                } else {
                    loadArticlesFromDatabase();
                }
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    errorText.setText("加载失败: " + error);
                    errorText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void loadArticlesFromDatabase() {
        repository.getAllArticles(new RssRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleEntity> articles) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    if (articles.isEmpty()) {
                        errorText.setText("还没有文章\n\n在订阅标签页中刷新订阅源");
                        errorText.setVisibility(View.VISIBLE);
                    } else {
                        errorText.setVisibility(View.GONE);
                    }

                    adapter.setArticles(articles);
                    isTranslated = false;
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    errorText.setText("加载失败: " + error);
                    errorText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void searchArticles(String query) {
        repository.searchArticles(query, new RssRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleEntity> articles) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setArticles(articles);
                    isTranslated = false;
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    errorText.setText("搜索失败: " + error);
                    errorText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadArticles();
    }
}
