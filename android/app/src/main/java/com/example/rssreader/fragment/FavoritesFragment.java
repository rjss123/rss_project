package com.example.rssreader.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyText;
    private RssRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new RssRepository(requireContext());

        initViews(view);
        setupRecyclerView();
        loadFavorites();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyText = view.findViewById(R.id.emptyText);

        swipeRefresh.setOnRefreshListener(this::loadFavorites);
    }

    private void setupRecyclerView() {
        adapter = new ArticleAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadFavorites() {
        repository.getFavoriteArticles(new RssRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleEntity> articles) {
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);

                    if (articles.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setArticles(articles);
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }
}
