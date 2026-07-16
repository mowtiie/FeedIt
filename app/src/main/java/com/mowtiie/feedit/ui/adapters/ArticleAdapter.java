package com.mowtiie.feedit.ui.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ItemArticleBinding;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.util.ArticleUiState;

import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class ArticleAdapter extends ListAdapter<ArticleUiState, ArticleAdapter.ViewHolder> {

    public interface Listener {
        void onArticleClicked(ArticleUiState item);

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

    public ArticleAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
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
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemArticleBinding binding;

        ViewHolder(ItemArticleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ArticleUiState item, Listener listener) {
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

            binding.buttonStar.setImageResource(article.isStarred()
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);

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

            binding.getRoot().setOnClickListener(v -> listener.onArticleClicked(item));
            binding.buttonStar.setOnClickListener(v -> listener.onStarToggled(item));
        }
    }
}