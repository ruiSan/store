package sdk.a71chat.com.demo.Download;

import android.os.Environment;

import java.io.File;

/**
 * Created by dell on 2018/3/6.
 */

public class FileInfo {

    private static final String DOWN_DIR = Environment.getExternalStorageDirectory() + File.separator + "client/";

    /**
     * 待下载的文件地址
     */
    private String downLoadUrl;
    /**
     * 下载在本地的文件名称
     */
    private String fileName;
    /**
     * 下载到本地的文件路径
     */
    private String filePath;

    public FileInfo(String downLoadUrl, String fileName) {
        this(downLoadUrl, fileName, DOWN_DIR);
    }

    public FileInfo(String downLoadUrl, String fileName, String filePath) {
        this.downLoadUrl = downLoadUrl;
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public String getDownLoadUrl() {
        return downLoadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * 判断是否已经下载完成
     * @return
     */
    public boolean hasDownLoaded() {
        File file = new File(filePath + fileName);
        if (!file.exists()) {
            //未下载
            return false;
        }
        return true;
    }
}
