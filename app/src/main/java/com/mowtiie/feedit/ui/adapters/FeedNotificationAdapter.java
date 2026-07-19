package com.mowtiie.feedit.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.feedit.databinding.ItemFeedNotificationBinding;
import com.mowtiie.feedit.model.FeedTags;

import java.util.Objects;

public class FeedNotificationAdapter extends ListAdapter<FeedTags, FeedNotificationAdapter.ViewHolder> {

    public interface Listener {
        void onNotifyNewToggled(FeedTags item, boolean enabled);
    }

    private static final DiffUtil.ItemCallback<FeedTags> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FeedTags>() {
                @Override
                public boolean areItemsTheSame(@NonNull FeedTags oldItem, @NonNull FeedTags newItem) {
                    return oldItem.getFeed().getId() == newItem.getFeed().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull FeedTags oldItem, @NonNull FeedTags newItem) {
                    return Objects.equals(oldItem.getFeed().getTitle(), newItem.getFeed().getTitle())
                            && oldItem.getFeed().isNotifyNew() == newItem.getFeed().isNotifyNew();
                }
            };

    private final Listener listener;

    public FeedNotificationAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFeedNotificationBinding binding =
                ItemFeedNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFeedNotificationBinding binding;

        ViewHolder(ItemFeedNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FeedTags item, Listener listener) {
            binding.textFeedTitle.setText(item.getFeed().getTitle() != null ? item.getFeed().getTitle() : item.getFeed().getUrl());

            binding.switchNotifyNew.setOnCheckedChangeListener(null);
            binding.switchNotifyNew.setChecked(item.getFeed().isNotifyNew());
            binding.switchNotifyNew.setOnCheckedChangeListener((button, checked) -> listener.onNotifyNewToggled(item, checked));
        }
    }
}