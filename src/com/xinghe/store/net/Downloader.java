package com.xinghe.store.net;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;

import com.xinghe.store.Utils;
import com.xinghe.store.XingheApp;

import java.io.File;

/**
 * 应用内文件下载器（API 3 无 DownloadManager，自实现）。
 * 下载完成后自动写入本地「下载记录」表。
 */
public final class Downloader {

    public interface Callback {
        /** error 为 null 表示成功 */
        void onFinished(File file, String mime, String error);
    }

    private Downloader() {
    }

    public static void download(final Activity activity, final String url,
                                final String fileName, final String appId,
                                final Callback cb) {
        final File dir = new File(Environment.getExternalStorageDirectory(), "xinghe");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        final String safeName = Utils.safeFileName(
                fileName != null && fileName.length() > 0 ? fileName : "xinghe_download");
        final File target = new File(dir, safeName);

        final ProgressDialog dlg = new ProgressDialog(activity);
        dlg.setMessage("正在下载 " + safeName + " …");
        dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dlg.setIndeterminate(true);
        dlg.setCancelable(true);
        dlg.show();

        new AsyncTask<Void, Integer, String>() {
            protected String doInBackground(Void... p) {
                try {
                    Http.downloadToFile(url, target.getAbsolutePath(),
                            new Http.ProgressCallback() {
                                public void onProgress(long done, long total) {
                                    publishProgress(total > 0 ? (int) (done * 100 / total) : -1);
                                }
                            });
                    return null;
                } catch (Exception e) {
                    return e.getMessage();
                }
            }

            protected void onProgressUpdate(Integer... values) {
                if (values != null && values.length > 0 && values[0] != null && values[0] >= 0) {
                    dlg.setIndeterminate(false);
                    dlg.setProgress(Math.max(1, values[0]));
                }
            }

            protected void onPostExecute(String error) {
                dlg.dismiss();
                if (error != null || !target.exists()) {
                    cb.onFinished(null, null, error != null ? error : "文件写入失败");
                    return;
                }
                String mime = Utils.mimeOfFile(target.getName());
                if (XingheApp.db != null) {
                    XingheApp.db.addDownload(appId, target.getName(),
                            target.getAbsolutePath(), mime, target.length());
                }
                cb.onFinished(target, mime, null);
            }
        }.execute();
    }
}
