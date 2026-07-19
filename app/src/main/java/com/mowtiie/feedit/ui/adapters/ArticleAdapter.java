package com.mowtiie.feedit.ui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ItemArticleCardBinding;
import com.mowtiie.feedit.databinding.ItemArticleCompactBinding;
import com.mowtiie.feedit.databinding.ItemArticleListBinding;
import com.mowtiie.feedit.databinding.ItemArticleMagazineBinding;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.PrefsKeys;

import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class ArticleAdapter extends ListAdapter<ArticleUiState, ArticleAdapter.ArticleViewHolder> {

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_COMPACT = 1;
    private static final int VIEW_TYPE_CARD = 2;
    private static final int VIEW_TYPE_MAGAZINE = 3;

    public interface Listener {
        void onArticleClicked(ArticleUiState item);
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
    private String layoutStyle = PrefsKeys.LAYOUT_CARD;

    public ArticleAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setLayoutStyle(String style) {
        if (!style.equals(layoutStyle)) {
            layoutStyle = style;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (PrefsKeys.LAYOUT_LIST.equals(layoutStyle)) {
            return VIEW_TYPE_LIST;
        } else if (PrefsKeys.LAYOUT_COMPACT.equals(layoutStyle)) {
            return VIEW_TYPE_COMPACT;
        } else if (PrefsKeys.LAYOUT_MAGAZINE.equals(layoutStyle)) {
            return VIEW_TYPE_MAGAZINE;
        } else {
            return VIEW_TYPE_CARD;
        }
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_LIST) {
            return new ListViewHolder(ItemArticleListBinding.inflate(inflater, parent, false));
        } else if (viewType == VIEW_TYPE_COMPACT) {
            return new CompactViewHolder(ItemArticleCompactBinding.inflate(inflater, parent, false));
        } else if (viewType == VIEW_TYPE_MAGAZINE) {
            return new MagazineViewHolder(ItemArticleMagazineBinding.inflate(inflater, parent, false));
        } else {
            return new CardViewHolder(ItemArticleCardBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    abstract static class ArticleViewHolder extends RecyclerView.ViewHolder {
        ArticleViewHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(ArticleUiState item, Listener listener);
    }

    private static void bindTitleAndMeta(MaterialTextView titleView, MaterialTextView metaView, ArticleUiState item) {
        Article article = item.getArticle();
        titleView.setText(article.getTitle());
        titleView.setTypeface(null, article.isRead() ? Typeface.NORMAL : Typeface.BOLD);

        String meta = item.getFeedTitle();
        if (article.getPublishedAt() != null) {
            String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(article.getPublishedAt()));
            meta = meta.isEmpty() ? date : meta + " · " + date;
        }
        metaView.setText(meta);
    }

    private static void applyReadAlpha(View root, Article article) {
        root.setAlpha(article.isRead() ? 0.6f : 1f);
    }

    private static void bindThumbnail(ImageView imageView, Article article) {
        if (article.getImageUrl() != null) {
            Glide.with(imageView.getContext())
                    .load(article.getImageUrl())
                    .placeholder(R.drawable.placeholder_thumbnail)
                    .centerCrop()
                    .into(imageView);
        } else {
            imageView.setImageDrawable(null);
            imageView.setBackgroundResource(R.drawable.placeholder_thumbnail);
        }
    }

    private static void bindStarAndClick(MaterialCardView card, ImageView starIndicator, ArticleUiState item, Listener listener) {
        Article article = item.getArticle();
        starIndicator.setVisibility(article.isStarred() ? View.VISIBLE : View.GONE);
        card.setCardBackgroundColor(resolveThemeColor(card.getContext(), com.google.android.material.R.attr.colorSurface));
        card.setOnClickListener(v -> listener.onArticleClicked(item));
    }

    private static int resolveThemeColor(Context context, int attrResId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

    static class ListViewHolder extends ArticleViewHolder {
        private final ItemArticleListBinding binding;

        ListViewHolder(ItemArticleListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        void bind(ArticleUiState item, Listener listener) {
            bindTitleAndMeta(binding.textTitle, binding.textMeta, item);
            applyReadAlpha(binding.getRoot(), item.getArticle());
            bindStarAndClick(binding.getRoot(), binding.imageStarIndicator, item, listener);
        }
    }

    static class CompactViewHolder extends ArticleViewHolder {
        private final ItemArticleCompactBinding binding;

        CompactViewHolder(ItemArticleCompactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        void bind(ArticleUiState item, Listener listener) {
            bindTitleAndMeta(binding.textTitle, binding.textMeta, item);
            applyReadAlpha(binding.getRoot(), item.getArticle());
            bindThumbnail(binding.imageThumbnail, item.getArticle());
            bindStarAndClick(binding.getRoot(), binding.imageStarIndicator, item, listener);
        }
    }

    static class CardViewHolder extends ArticleViewHolder {
        private final ItemArticleCardBinding binding;

        CardViewHolder(ItemArticleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        void bind(ArticleUiState item, Listener listener) {
            bindTitleAndMeta(binding.textTitle, binding.textMeta, item);
            applyReadAlpha(binding.getRoot(), item.getArticle());
            bindThumbnail(binding.imageThumbnail, item.getArticle());
            bindStarAndClick(binding.getRoot(), binding.imageStarIndicator, item, listener);
        }
    }

    static class MagazineViewHolder extends ArticleViewHolder {
        private final ItemArticleMagazineBinding binding;

        MagazineViewHolder(ItemArticleMagazineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        void bind(ArticleUiState item, Listener listener) {
            bindTitleAndMeta(binding.textTitle, binding.textMeta, item);
            applyReadAlpha(binding.getRoot(), item.getArticle());
            bindThumbnail(binding.imageThumbnail, item.getArticle());
            bindStarAndClick(binding.getRoot(), binding.imageStarIndicator, item, listener);
        }
    }
}