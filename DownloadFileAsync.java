package com.sevenonechat.sdk.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.qysn.cj.api.http.lytokhttp3.ResponseBody;
import com.qysn.cj.api.http.lytretrofit.Call;
import com.qysn.cj.api.http.lytretrofit.Response;
import com.sevenonechat.sdk.http.RequestApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by dell on 2018/2/2.
 */

public class DownloadFileAsync extends AsyncTask<String, Integer, String> {

    ProgressDialog dialog;
    private Call<ResponseBody> call;
    private Activity activity;
    private DownLoadInter downLoadInter;


    public DownloadFileAsync(Activity activity) {
        this.activity = activity;
    }

    public void setDownLoadInter(DownLoadInter downLoadInter) {
        this.downLoadInter = downLoadInter;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(activity);
        dialog.setTitle("提示");
        dialog.setMessage("正在下载,按返回键可取消");
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (null != call) {
                    call.cancel();
                }
            }
        });

        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setMax(100);
        dialog.setCancelable(true);// 设置是否可以通过点击Back键取消
        dialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
        dialog.show();
    }

    @Override
    protected String doInBackground(String... params) {
        call = RequestApi.downloadByStreaming(params[0]);
        try {
            Response<ResponseBody> response = call.execute();
            if (null != response && response.isSuccessful()) {
//                String destFilename = "chat_new.apk";
//                File destFile = new File(params[1]);
                boolean writtenToDisk = writeResponseBodyToDisk(response.body(), params[1]);
                if (writtenToDisk) {
                    dialog.dismiss();
                    if (null != downLoadInter) {
                        downLoadInter.downLoadSuccess();
                        activity = null;
                    }
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("下载失败");
                            if (null != downLoadInter) {
                                downLoadInter.downLoadSuccess();
                            }
                            activity = null;
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.setMessage("下载失败");
                    if (null != downLoadInter) {
                        downLoadInter.downLoadSuccess();
                    }
                    activity = null;
                }
            });
        }
        return null;
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String destFilename) {
        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.setProgress(5);
                }
            });

            try {
                byte[] fileReader = new byte[4096];
                L.e("", "writeResponseBodyToDisk()");
                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                L.e("", "writeResponseBodyToDisk()2");
                inputStream = body.source().inputStream();
//                File file = new File(destFilename);
//                if (!file.exists()) {
//                    file.mkdir();
//                }
                outputStream = new FileOutputStream(destFilename);
//                        activity.openFileOutput(destFilename, Context.MODE_WORLD_READABLE);
                L.e("", "writeResponseBodyToDisk()3");
                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    final int percent = (int) (100 * (fileSizeDownloaded * 1.0f / fileSize));
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(percent);
                        }
                    });

                }

                outputStream.flush();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        dialog.setProgress(progress[0]);
    }

    public interface DownLoadInter {
        public void downLoadSuccess();

        public void downLoadFail();
    }
}
