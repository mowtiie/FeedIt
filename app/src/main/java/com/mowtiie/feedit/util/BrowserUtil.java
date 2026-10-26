package com.mowtiie.feedit.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.mowtiie.feedit.R;

public final class BrowserUtil {

    private BrowserUtil() {
    }

    /** Opens a URL in the user's browser, or shows a toast when no browser is installed. */
    public static void openUrl(Context context, String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
        }
    }
}
