package com.mowtiie.feedit.ui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ItemArticleBinding;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.util.ArticleUiState;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class ArticleAdapter extends ListAdapter<ArticleUiState, ArticleAdapter.ViewHolder> {

    public interface Listener {
        void onArticleClicked(ArticleUiState item);

        void onArticleLongClicked(ArticleUiState item);

        void onStarToggled(ArticleUiState item);
    }

    private static final DiffUtil.ItemCallback<ArticleUiState> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ArticleUiState>() {
                @Override
                public boolean areItemsTheSame(@NonNull ArticleUiState oldItem, @NonNull ArticleUiState newItem) {
                    return oldItem.getArticle().getId() == newItem.getArticle().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ArticleUiState oldItem, @NonNull ArticleUiState newItem) {
                    Article a = oldItem.getArticle();
                    Article b = newItem.getArticle();
                    return a.isRead() == b.isRead()
                            && a.isStarred() == b.isStarred()
                            && Objects.equals(a.getTitle(), b.getTitle())
                            && Objects.equals(a.getImageUrl(), b.getImageUrl())
                            && Objects.equals(oldItem.getFeedTitle(), newItem.getFeedTitle());
                }
            };

    private final Listener listener;
    private Set<Long> selectedIds = Collections.emptySet();
    private boolean selectionMode = false;

    public ArticleAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void updateSelection(Set<Long> selectedIds, boolean selectionMode) {
        this.selectedIds = selectedIds;
        this.selectionMode = selectionMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemArticleBinding binding =
                ItemArticleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArticleUiState item = getItem(position);
        boolean isSelected = selectedIds.contains(item.getArticle().getId());
        holder.bind(item, listener, selectionMode, isSelected);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemArticleBinding binding;

        ViewHolder(ItemArticleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ArticleUiState item, Listener listener, boolean selectionMode, boolean isSelected) {
            Article article = item.getArticle();

            binding.textTitle.setText(article.getTitle());
            binding.textTitle.setTypeface(null, article.isRead() ? Typeface.NORMAL : Typeface.BOLD);
            binding.getRoot().setAlpha(article.isRead() ? 0.6f : 1f);

            String meta = item.getFeedTitle();
            if (article.getPublishedAt() != null) {
                String date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                        .format(new Date(article.getPublishedAt()));
                meta = meta.isEmpty() ? date : meta + " · " + date;
            }
            binding.textMeta.setText(meta);

            if (article.getImageUrl() != null) {
                Glide.with(binding.imageThumbnail.getContext())
                        .load(article.getImageUrl())
                        .placeholder(R.drawable.placeholder_thumbnail)
                        .centerCrop()
                        .into(binding.imageThumbnail);
            } else {
                binding.imageThumbnail.setImageDrawable(null);
                binding.imageThumbnail.setBackgroundResource(R.drawable.placeholder_thumbnail);
            }

            MaterialCardView card = binding.getRoot();
            if (selectionMode) {
                binding.buttonStar.setVisibility(android.view.View.GONE);
                binding.checkboxSelect.setVisibility(android.view.View.VISIBLE);
                binding.checkboxSelect.setChecked(isSelected);
                card.setCardBackgroundColor(resolveThemeColor(card.getContext(),
                        isSelected ? com.google.android.material.R.attr.colorPrimaryContainer
                                : com.google.android.material.R.attr.colorSurface));
            } else {
                binding.buttonStar.setVisibility(android.view.View.VISIBLE);
                binding.checkboxSelect.setVisibility(android.view.View.GONE);
                binding.buttonStar.setImageResource(article.isStarred()
                        ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off);
                card.setCardBackgroundColor(
                        resolveThemeColor(card.getContext(), com.google.android.material.R.attr.colorSurface));
            }

            card.setOnClickListener(v -> listener.onArticleClicked(item));
            card.setOnLongClickListener(v -> {
                listener.onArticleLongClicked(item);
                return true;
            });
            binding.buttonStar.setOnClickListener(v -> listener.onStarToggled(item));
        }

        private static int resolveThemeColor(Context context, int attrResId) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(attrResId, typedValue, true);
            return typedValue.data;
        }
    }
}