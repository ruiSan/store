package sdk.a71chat.com.demo.Download;

import android.text.TextUtils;

import com.qysn.cj.api.http.lytokhttp3.Call;
import com.qysn.cj.api.http.lytokhttp3.Callback;
import com.qysn.cj.api.http.lytokhttp3.Response;
import com.sevenonechat.sdk.util.ToastUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import sdk.a71chat.com.demo.DemoApp;

/**
 * Created by dell on 2018/3/6.
 * 下载任务
 */
public class DownLoadTask {
    /**
     * 等待下载
     */
    private static final int WAITING = 0;
    /**
     * 正在下载中
     */
    private static final int LOADING = 1;
    /**
     * 下载中断
     */
    private static final int PENDING = 2;
    /**
     * 下载失败
     */
    private static final int FAIL = 3;
    /**
     * 下载完成
     */
    private static final int SUCCESS = 4;

    /**
     * 开启的线程数量
     */
    private final int THREAD_COUNT = 3;

    /**
     * 待下载的文件信息
     */
    private FileInfo fileInfo;

    private int downLodingStatus = WAITING;

    private long mFileLength = 0l;

    /**
     * 下载完成计数器
     */
    private int downLoadingCount = 0;

    private File tempFile;

    public DownLoadTask(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    /**
     * 本task开始执行
     */
    public void taskRun() {
        if (TextUtils.isEmpty(fileInfo.getDownLoadUrl())) {
            downLodingStatus = FAIL;
            DownLoadManager.getInstance().removeTask(this);
            return;
        }
        if (fileInfo.hasDownLoaded()) {
            downLodingStatus = SUCCESS;
            DownLoadManager.getInstance().removeTask(this);
            return;
        }
        downLodingStatus = LOADING;
        if (0l == mFileLength) {
            getFileTotalLength();
        } else {
            startDownLoad();
        }
    }

    /**
     * 获取待下载的文件总大小
     */
    private void getFileTotalLength() {
        try {
            OkhttpUtil.getInstance().getContentLength(fileInfo.getDownLoadUrl(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() != 200) {
                        close(response.body());
                        return;
                    }
                    mFileLength = response.body().contentLength();
                    close(response.body());
                    startDownLoad();
                }
            });
        } catch (Exception e) {

        }
    }

    /**
     * 执行下载操作
     */
    private void startDownLoad() {
        //计数器归0
        downLoadingCount = 0;
        tempFile = new File(fileInfo.getFilePath(), fileInfo.getFileName() + ".tmp");
        if (!tempFile.getParentFile().exists()) {
            tempFile.getParentFile().mkdirs();
        }
        try {
            RandomAccessFile tmpAccessFile = new RandomAccessFile(tempFile, "rwd");
            tmpAccessFile.setLength(mFileLength);
            tmpAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long blockSize = mFileLength/THREAD_COUNT;
        for (int i = 0; i < THREAD_COUNT; i ++) {
            long startIndex = i * blockSize;
            long endIndex = (i + 1) * blockSize - 1;
            if (i == THREAD_COUNT - 1) {
                endIndex = mFileLength - 1;
            }
            doDownLoad(startIndex, endIndex, i);
        }
    }

    /**
     * 开始下载
     * @param startIndex
     * @param endIndex
     * @param threadId
     */
    private void doDownLoad(final long startIndex, final long endIndex, final int threadId) {
        long newStartIndex = startIndex;
        final File cacheFile = new File(fileInfo.getFilePath(), "thread" + threadId + "_" + fileInfo.getFileName() + ".cache");
        try {
            final RandomAccessFile cacheAccessFile = new RandomAccessFile(cacheFile, "rwd");
            if (cacheFile.exists()) {// 如果文件存在
                String startIndexStr = cacheAccessFile.readLine();
                newStartIndex = Integer.parseInt(startIndexStr);//重新设置下载起点
            }
            if (newStartIndex == endIndex) {
                close(cacheAccessFile);
                downLoadingCount ++;
                return;
            }
            final long lastStatIndex = newStartIndex;
            OkhttpUtil.getInstance().downloadFileByRange(fileInfo.getDownLoadUrl(), lastStatIndex, endIndex, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() != 206) {// 206：请求部分资源成功码，表示服务器支持断点续传
                        return;
                    }
                    InputStream is = response.body().byteStream();// 获取流
                    RandomAccessFile tmpAccessFile = new RandomAccessFile(tempFile, "rw");// 获取前面已创建的文件.
                    tmpAccessFile.seek(lastStatIndex);// 文件写入的开始位置.
             /*  将网络流中的文件写入本地*/
                    byte[] buffer = new byte[1024 << 2];
                    int length = -1;
                    int total = 0;// 记录本次下载文件的大小
                    long progress = 0;
                    while ((length = is.read(buffer)) > 0) {//读取流
                        tmpAccessFile.write(buffer, 0, length);
                        total += length;
                        progress = lastStatIndex + total;
                        //将该线程最新完成下载的位置记录并保存到缓存数据文件中
                        //建议转成Base64码，防止数据被修改，导致下载文件出错（若真有这样的情况，这样的朋友可真是无聊透顶啊）
                        cacheAccessFile.seek(0);
                        cacheAccessFile.write((progress + "").getBytes("UTF-8"));
                    }
                    //关闭资源
                    close(cacheAccessFile, is, response.body());
                    // 删除临时文件
                    cleanFile(cacheFile);
                    downLoadingCount ++;
                    processEnd();
                }
            });
        } catch (Exception e) {

        }
    }

    private void processEnd() {
        if (downLoadingCount == THREAD_COUNT) {
            ToastUtil.showTips(DemoApp.mContext, "下载完成");
        }
    }

    /**
     * 删除临时文件
     */
    private void cleanFile(File... files) {
        for (int i = 0, length = files.length; i < length; i++) {
            if (null != files[i])
                files[i].delete();
        }
    }

    /**
     * 关闭资源
     *
     * @param closeables
     */
    private void close(Closeable... closeables) {
        int length = closeables.length;
        try {
            for (int i = 0; i < length; i++) {
                Closeable closeable = closeables[i];
                if (null != closeable)
                    closeables[i].close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            for (int i = 0; i < length; i++) {
                closeables[i] = null;
            }
        }
    }

    public int getRunStatus() {
        return downLodingStatus;
    }
}
