package com.mowtiie.feedit.crash;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.mowtiie.feedit.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashReporter {

    private static final String REPORT_FILENAME = "last_crash.txt";

    public static File getReportFile(Context context) {
        return new File(context.getFilesDir(), REPORT_FILENAME);
    }

    public static boolean hasReport(Context context) {
        return getReportFile(context).exists();
    }

    @Nullable
    public static String readReport(Context context) {
        File file = getReportFile(context);
        if (!file.exists()) return null;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public static void deleteReport(Context context) {
        File file = getReportFile(context);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void showDialogIfPending(Activity activity, ActivityResultLauncher<Intent> saveLauncher) {
        if (!hasReport(activity)) return;

        View crashDialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_crash, null, false);

        MaterialButton actionCopy = crashDialogView.findViewById(R.id.crash_action_copy);
        MaterialButton actionExport = crashDialogView.findViewById(R.id.crash_action_export);
        MaterialButton actionDismiss = crashDialogView.findViewById(R.id.crash_action_dismiss);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_title_crash)
                .setMessage(R.string.dialog_message_crash)
                .setCancelable(false)
                .setView(crashDialogView);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            actionCopy.setOnClickListener(view -> {
                String report = readReport(activity);
                if (report != null && copyToClipboard(activity, report)) {
                    Toast.makeText(activity, R.string.toast_crash_copy_success, Toast.LENGTH_SHORT).show();
                    deleteReport(activity);
                }
                dialog.dismiss();
            });

            actionExport.setOnClickListener(view -> {
                launchExportDialog(saveLauncher);
                dialog.dismiss();
            });

            actionDismiss.setOnClickListener(view -> {
                deleteReport(activity);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    public static boolean writeReportToUri(Context context, Uri uri) {
        String report = readReport(context);
        if (report == null) return false;

        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out == null) return false;
            out.write(report.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String suggestedFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        return "feedit_crash_" + sdf.format(new Date()) + ".txt";
    }

    private static boolean copyToClipboard(Context context, String report) {
        ClipboardManager clipboard = context.getSystemService(ClipboardManager.class);
        if (clipboard == null) return false;

        ClipData clip = ClipData.newPlainText(
                context.getString(R.string.crash_clip_label), report);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }

        clipboard.setPrimaryClip(clip);
        return true;
    }

    private static void launchExportDialog(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, suggestedFilename());
        launcher.launch(intent);
    }
}