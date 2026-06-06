package com.example.rssreader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rssreader.R;
import com.example.rssreader.database.FeedEntity;
import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private List<FeedEntity> feeds = new ArrayList<>();
    private Context context;
    private OnFeedClickListener clickListener;
    private OnFeedDeleteListener deleteListener;

    public interface OnFeedClickListener {
        void onClick(FeedEntity feed);
    }

    public interface OnFeedDeleteListener {
        void onDelete(FeedEntity feed);
    }

    public FeedAdapter(Context context, OnFeedClickListener clickListener, OnFeedDeleteListener deleteListener) {
        this.context = context;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void setFeeds(List<FeedEntity> feeds) {
        this.feeds = feeds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        FeedEntity feed = feeds.get(position);
        holder.bind(feed);
    }

    @Override
    public int getItemCount() {
        return feeds.size();
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText;
        private TextView urlText;
        private Button btnRefresh;
        private Button btnDelete;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.feedTitle);
            urlText = itemView.findViewById(R.id.feedUrl);
            btnRefresh = itemView.findViewById(R.id.btnRefresh);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(FeedEntity feed) {
            titleText.setText(feed.getTitle());
            urlText.setText(feed.getUrl());

            btnRefresh.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(feed);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(feed);
                }
            });
        }
    }
}
