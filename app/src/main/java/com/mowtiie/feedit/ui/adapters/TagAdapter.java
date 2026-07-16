package com.mowtiie.feedit.ui.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.feedit.databinding.ItemTagManagementBinding;
import com.mowtiie.feedit.model.Tag;

import java.util.Objects;

public class TagAdapter extends ListAdapter<Tag, TagAdapter.ViewHolder> {

    public interface Listener {
        void onEditClicked(Tag tag);

        void onDeleteClicked(Tag tag);
    }

    private static final DiffUtil.ItemCallback<Tag> DIFF_CALLBACK = new DiffUtil.ItemCallback<Tag>() {
        @Override
        public boolean areItemsTheSame(@NonNull Tag oldItem, @NonNull Tag newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Tag oldItem, @NonNull Tag newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && Objects.equals(oldItem.getColor(), newItem.getColor());
        }
    };

    private final Listener listener;

    public TagAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTagManagementBinding binding =
                ItemTagManagementBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemTagManagementBinding binding;

        ViewHolder(ItemTagManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Tag tag, Listener listener) {
            binding.textTagName.setText(tag.getName());
            if (tag.getColor() != null) {
                try {
                    binding.viewTagColor.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor(tag.getColor())));
                } catch (IllegalArgumentException ignored) {
                }
            }
            binding.buttonEditTag.setOnClickListener(v -> listener.onEditClicked(tag));
            binding.buttonDeleteTag.setOnClickListener(v -> listener.onDeleteClicked(tag));
        }
    }
}
