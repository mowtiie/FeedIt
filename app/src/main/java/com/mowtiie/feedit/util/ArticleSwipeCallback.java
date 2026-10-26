package com.mowtiie.feedit.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.color.MaterialColors;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.ui.adapters.ArticleAdapter;

/** Swipe right toggles read, swipe left toggles starred. */
public class ArticleSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface Actions {
        void onSwipeToggleRead(ArticleUiState item);

        void onSwipeToggleStar(ArticleUiState item);
    }

    private final ArticleAdapter adapter;
    private final Actions actions;
    private final Drawable readIcon;
    private final Drawable starIcon;
    private final int readBackground;
    private final int starBackground;
    private final int iconMargin;
    private RecyclerView recyclerView;
    private ItemTouchHelper helper;

    /**
     * Attaches swipe handling to the list. Change animations are turned off so a toggled row is
     * rebound in place instead of crossfading with its swiped-out copy.
     */
    public static void attach(RecyclerView recyclerView, ArticleAdapter adapter, Actions actions) {
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        ArticleSwipeCallback callback = new ArticleSwipeCallback(recyclerView.getContext(), adapter, actions);
        callback.recyclerView = recyclerView;
        callback.helper = new ItemTouchHelper(callback);
        callback.helper.attachToRecyclerView(recyclerView);
    }

    public ArticleSwipeCallback(Context context, ArticleAdapter adapter, Actions actions) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.actions = actions;
        this.iconMargin = (int) (24 * context.getResources().getDisplayMetrics().density);

        readBackground = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, 0);
        starBackground = MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiaryContainer, 0);
        readIcon = tintedIcon(context, R.drawable.ic_check,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, 0));
        starIcon = tintedIcon(context, R.drawable.ic_star_filled,
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnTertiaryContainer, 0));
    }

    private static Drawable tintedIcon(Context context, int resId, int color) {
        Drawable icon = AppCompatResources.getDrawable(context, resId);
        if (icon == null) {
            return null;
        }
        icon = DrawableCompat.wrap(icon.mutate());
        DrawableCompat.setTint(icon, color);
        return icon;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position >= adapter.getCurrentList().size()) {
            return;
        }
        ArticleUiState item = adapter.getCurrentList().get(position);
        if (direction == ItemTouchHelper.RIGHT) {
            actions.onSwipeToggleRead(item);
        } else {
            actions.onSwipeToggleStar(item);
        }
        // The row stays in the list, but ItemTouchHelper only cleans up a swiped row when it is
        // removed, so it would stay drawn off-screen. Re-attaching forces that cleanup.
        adapter.notifyItemChanged(position);
        recyclerView.post(() -> {
            helper.attachToRecyclerView(null);
            helper.attachToRecyclerView(recyclerView);
        });
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setTranslationX(0f);
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        if (dX != 0f) {
            boolean swipingRight = dX > 0;
            ColorDrawable background = new ColorDrawable(swipingRight ? readBackground : starBackground);
            if (swipingRight) {
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + (int) dX, itemView.getBottom());
            } else {
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());
            }
            background.draw(canvas);

            Drawable icon = swipingRight ? readIcon : starIcon;
            if (icon != null) {
                int size = icon.getIntrinsicHeight();
                int top = itemView.getTop() + (itemView.getHeight() - size) / 2;
                int left = swipingRight
                        ? itemView.getLeft() + iconMargin
                        : itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                icon.setBounds(left, top, left + icon.getIntrinsicWidth(), top + size);
                icon.draw(canvas);
            }
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
