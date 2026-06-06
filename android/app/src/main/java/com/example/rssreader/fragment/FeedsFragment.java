package com.example.rssreader.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.rssreader.R;
import com.example.rssreader.RssRepository;
import com.example.rssreader.AIConfigActivity;
import com.example.rssreader.adapter.FeedAdapter;
import com.example.rssreader.database.FeedEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class FeedsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabAddFeed;
    private FloatingActionButton fabAIConfig;
    private TextView emptyText;
    private RssRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feeds, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new RssRepository(requireContext());

        initViews(view);
        setupRecyclerView();
        loadFeeds();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.feedRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        fabAddFeed = view.findViewById(R.id.fabAddFeed);
        fabAIConfig = view.findViewById(R.id.fabAIConfig);
        emptyText = view.findViewById(R.id.emptyText);

        swipeRefresh.setOnRefreshListener(this::loadFeeds);
        fabAddFeed.setOnClickListener(v -> showAddFeedDialog());
        fabAIConfig.setOnClickListener(v -> openAIConfig());
    }

    private void openAIConfig() {
        Intent intent = new Intent(requireContext(), AIConfigActivity.class);
        startActivity(intent);
    }

    private void setupRecyclerView() {
        adapter = new FeedAdapter(requireContext(), this::onFeedRefresh, this::onFeedDelete);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadFeeds() {
        repository.getAllFeeds(new RssRepository.OnFeedsLoadedListener() {
            @Override
            public void onSuccess(List<FeedEntity> feeds) {
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);

                    if (feeds.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setFeeds(feeds);
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), "加载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showAddFeedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("添加 RSS 订阅");

        final EditText input = new EditText(requireContext());
        input.setHint("输入 RSS URL");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("添加", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                addFeed(url);
            } else {
                Toast.makeText(requireContext(), "URL 不能为空", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addFeed(String url) {
        Toast.makeText(requireContext(), "正在添加订阅...", Toast.LENGTH_SHORT).show();

        repository.addFeed(url, new RssRepository.OnFeedOperationListener() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadFeeds();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void onFeedRefresh(FeedEntity feed) {
        Toast.makeText(requireContext(), "正在刷新...", Toast.LENGTH_SHORT).show();

        repository.refreshFeed(feed, new RssRepository.OnFeedOperationListener() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onFeedDelete(FeedEntity feed) {
        new AlertDialog.Builder(requireContext())
            .setTitle("删除订阅")
            .setMessage("确定要删除 \"" + feed.getTitle() + "\" 吗？")
            .setPositiveButton("删除", (dialog, which) -> deleteFeed(feed))
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteFeed(FeedEntity feed) {
        repository.deleteFeed(feed, new RssRepository.OnFeedOperationListener() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadFeeds();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
