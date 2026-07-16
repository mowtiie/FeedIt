package com.mowtiie.feedit.ui.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.mowtiie.feedit.databinding.ItemFeedManagementBinding;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;

import java.util.Objects;

public class FeedAdapter extends ListAdapter<FeedTags, FeedAdapter.ViewHolder> {

    public interface Listener {
        void onFeedClicked(FeedTags item);

        void onEditClicked(FeedTags item);

        void onDeleteClicked(FeedTags item);
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
                            && Objects.equals(oldItem.getFeed().getUrl(), newItem.getFeed().getUrl())
                            && tagSignature(oldItem).equals(tagSignature(newItem));
                }

                private String tagSignature(FeedTags item) {
                    StringBuilder sb = new StringBuilder();
                    for (Tag tag : item.getTags()) {
                        sb.append(tag.getId()).append(':').append(tag.getName()).append(',');
                    }
                    return sb.toString();
                }
            };

    private final Listener listener;

    public FeedAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFeedManagementBinding binding =
                ItemFeedManagementBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFeedManagementBinding binding;

        ViewHolder(ItemFeedManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FeedTags item, Listener listener) {
            binding.textFeedTitle.setText(item.getFeed().getTitle() != null
                    ? item.getFeed().getTitle() : item.getFeed().getUrl());
            binding.textFeedUrl.setText(item.getFeed().getUrl());

            binding.chipGroupFeedTags.removeAllViews();
            for (Tag tag : item.getTags()) {
                Chip chip = new Chip(binding.getRoot().getContext());
                chip.setText(tag.getName());
                chip.setClickable(false);
                chip.setCheckable(false);
                if (tag.getColor() != null) {
                    try {
                        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(tag.getColor())));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                binding.chipGroupFeedTags.addView(chip);
            }

            binding.getRoot().setOnClickListener(v -> listener.onFeedClicked(item));
            binding.buttonEditFeed.setOnClickListener(v -> listener.onEditClicked(item));
            binding.buttonDeleteFeed.setOnClickListener(v -> listener.onDeleteClicked(item));
        }
    }
}