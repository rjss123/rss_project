package com.example.rssreader.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private TextView tabAll, tabTech, tabLife;
    private int selectedTab = 0; // 0=全部, 1=科技, 2=生活
    private FloatingActionButton fabTranslate;
    private FloatingActionButton fabScrollTop;
    private RssRepository repository;
    private boolean isTranslated = false;
    private List<ArticleEntity> allArticles = new ArrayList<>();
    private List<FeedEntity> allFeeds = new ArrayList<>();

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
        setupCategoryTabs();
        setupTranslateButton();
        loadArticles();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        errorText = view.findViewById(R.id.errorText);
        tabAll = view.findViewById(R.id.tabAll);
        tabTech = view.findViewById(R.id.tabTech);
        tabLife = view.findViewById(R.id.tabLife);
        fabTranslate = view.findViewById(R.id.fabTranslate);
        fabScrollTop = view.findViewById(R.id.fabScrollTop);

        swipeRefresh.setOnRefreshListener(this::refreshAllFeeds);
    }

    private void setupRecyclerView() {
        adapter = new ArticleAdapter(requireContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // 滚动监听：向下滚动较多时显示"返回顶部"按钮
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (layoutManager.findFirstVisibleItemPosition() > 5) {
                    fabScrollTop.show();
                } else {
                    fabScrollTop.hide();
                }
            }
        });

        // 点击返回顶部
        fabScrollTop.setOnClickListener(v -> recyclerView.smoothScrollToPosition(0));
    }

    private void setupCategoryTabs() {
        tabAll.setOnClickListener(v -> selectTab(0));
        tabTech.setOnClickListener(v -> selectTab(1));
        tabLife.setOnClickListener(v -> selectTab(2));
    }

    private void selectTab(int tab) {
        selectedTab = tab;
        // 更新选中样式
        tabAll.setTextColor(tab == 0 ? 0xFF2c3e50 : 0xFF7f8c8d);
        tabAll.setTypeface(null, tab == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabTech.setTextColor(tab == 1 ? 0xFF2c3e50 : 0xFF7f8c8d);
        tabTech.setTypeface(null, tab == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabLife.setTextColor(tab == 2 ? 0xFF2c3e50 : 0xFF7f8c8d);
        tabLife.setTypeface(null, tab == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        applyCategoryFilter(tab);
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
                        allFeeds = new ArrayList<>();
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        errorText.setText("还没有订阅源\n\n点击右侧 [订阅] 标签页添加");
                        errorText.setVisibility(View.VISIBLE);
                        adapter.setArticles(new ArrayList<>());
                    });
                } else {
                    allFeeds = feeds;
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

    private void refreshAllFeeds() {
        swipeRefresh.setRefreshing(true);

        repository.refreshAllFeeds(new RssRepository.OnFeedOperationListener() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(HomeFragment.this::loadArticlesFromDatabase);
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    if (allArticles.isEmpty()) {
                        errorText.setText(error);
                        errorText.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadArticlesFromDatabase() {
        // 后台同步分类标签
        repository.syncFeedCategories();

        repository.getAllArticles(new RssRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleEntity> articles) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    if (articles.isEmpty()) {
                        errorText.setText("还没有文章\n\n在首页下拉刷新订阅源");
                        errorText.setVisibility(View.VISIBLE);
                    }

                    allArticles = articles;
                    applyCategoryFilter(selectedTab);
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

    private static final String[] TABS = {"全部", "科技", "生活"};

    private void applyCategoryFilter(int position) {
        if (position <= 0) {
            adapter.setArticles(allArticles);
            if (allArticles.isEmpty()) {
                errorText.setText("还没有文章\n\n在首页下拉刷新订阅源");
                errorText.setVisibility(View.VISIBLE);
            } else {
                errorText.setVisibility(View.GONE);
            }
            return;
        }

        String selectedCategory = TABS[position];
        List<ArticleEntity> filteredArticles = new ArrayList<>();
        for (ArticleEntity article : allArticles) {
            FeedEntity feed = getFeedById(article.getFeedId());
            if (feed != null && selectedCategory.equals(getFeedCategory(feed))) {
                filteredArticles.add(article);
            }
        }

        adapter.setArticles(filteredArticles);
        if (filteredArticles.isEmpty()) {
            errorText.setText("该分类还没有文章\n\n在首页下拉刷新订阅源");
            errorText.setVisibility(View.VISIBLE);
        } else {
            errorText.setVisibility(View.GONE);
        }
        isTranslated = false;
    }

    private FeedEntity getFeedById(int feedId) {
        for (FeedEntity feed : allFeeds) {
            if (feed.getId() == feedId) {
                return feed;
            }
        }
        return null;
    }

    private String getFeedCategory(FeedEntity feed) {
        if (feed.getDescription() != null && !feed.getDescription().isEmpty()) {
            return feed.getDescription();
        }
        return "未分类";
    }

    @Override
    public void onResume() {
        super.onResume();
        loadArticles();
    }
}
