package com.mowtiie.feedit.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textview.MaterialTextView;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.ui.viewmodel.MainViewModel;

public final class ArticleActionsBottomSheet {

    private ArticleActionsBottomSheet() {
    }

    public static void show(Activity activity, Article article, MainViewModel viewModel) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View sheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_article_actions, null, false);
        dialog.setContentView(sheetView);

        MaterialTextView sheetTitle = sheetView.findViewById(R.id.text_sheet_title);
        sheetTitle.setText(article.getTitle());

        ImageView iconReadState = sheetView.findViewById(R.id.icon_mark_read_state);
        MaterialTextView textReadState = sheetView.findViewById(R.id.text_mark_read_state);
        if (article.isRead()) {
            textReadState.setText(R.string.action_mark_unread);
            iconReadState.setImageResource(R.drawable.ic_check_remove);
        } else {
            textReadState.setText(R.string.action_mark_read);
            iconReadState.setImageResource(R.drawable.ic_check);
        }
        sheetView.findViewById(R.id.row_mark_read_state).setOnClickListener(v -> {
            viewModel.toggleRead(article);
            dialog.dismiss();
        });

        ImageView iconStarState = sheetView.findViewById(R.id.icon_star_state);
        MaterialTextView textStarState = sheetView.findViewById(R.id.text_star_state);
        if (article.isStarred()) {
            iconStarState.setImageResource(R.drawable.ic_star_filled);
            textStarState.setText(R.string.action_unstar);
        } else {
            iconStarState.setImageResource(R.drawable.ic_star_outlined);
            textStarState.setText(R.string.action_star);
        }
        sheetView.findViewById(R.id.row_star_state).setOnClickListener(v -> {
            viewModel.toggleStarred(article);
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.row_share).setOnClickListener(v -> {
            shareArticle(activity, article);
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.row_open_browser).setOnClickListener(v -> {
            openInBrowser(activity, article.getLink());
            dialog.dismiss();
        });

        dialog.show();
    }

    private static void shareArticle(Activity activity, Article article) {
        if (article.getLink() == null) {
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, article.getLink());
        if (article.getTitle() != null) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, article.getTitle());
        }
        activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.action_share)));
    }

    private static void openInBrowser(Activity activity, String url) {
        if (url == null) {
            return;
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "No browser app found", Toast.LENGTH_SHORT).show();
        }
    }
}