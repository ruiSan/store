package com.sevenonechat.sdk.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.qysn.cj.bean.msg.LYTMessage;
import com.qysn.cj.bean.msg.LYTTimeUtils;
import com.qysn.cj.bean.msg.LYTUtils;
import com.sevenonechat.sdk.bean.ChatMsgDao;
import com.sevenonechat.sdk.model.ChatMessage;
import com.sevenonechat.sdk.model.SessionId;
import com.sevenonechat.sdk.sdkinfo.SdkRunningClient;
import com.sevenonechat.sdk.service.ChatService;
import com.sevenonechat.sdk.service.Constants;
import com.sevenonechat.sdk.service.MsgClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dell on 2018/1/31.
 */

public class SoLoadUtil {

    /**
     * 是否具有远程分支代码
     * @return
     */
    public static boolean hasRemoteCode(Context context) {
        try {
            Class.forName("com.oray.sunlogin.servicesdk.jni.ClientServiceSDK");
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static void sendRemoteRequest(int code) {
        String sessionId = RemoteStartManager.getInstance().getCurrentSessionId();
        if (TextUtils.isEmpty(sessionId)) {
            return;
        }
        sendRemoteRequest(sessionId, code);
    }

    public static void sendRemoteRequest(String sessionId, int code) {
        JSONObject json1 = new JSONObject();
        json1.put("flag", code);

        JSONObject json = new JSONObject();
        json.put("cmpcd", Constants.getCompanyCode());
        json.put("seId", sessionId);
        json.put("vName", Constants.getVistorName());
        json.put("curcs", 0);
        json.put("curcsCode", "");
        json.put("itp", 9);
        json.put("wds", json1.toString());

        final LYTMessage message = LYTMessage.createTxtSendMessage(json.toJSONString(), Constants.getRoomId());
        message.setChatType(LYTMessage.ChatType.ChatRoom.ordinal());
        message.getLytObject().setMsgId("M" + LYTTimeUtils.getLocalTime() + 3 + LYTUtils.getRandom(8));
        MsgClient.getCJClient().getChatManager().sendMessage(message);
    }

    public static void sendClientRequest(int code, String address, String sessionName) {
        JSONObject json = new JSONObject();
        json.put("flag", code);
        json.put("sessionid", Constants.getRoomId());
        json.put("address", address);
        json.put("sessionname", sessionName);
        SdkRunningClient.getInstance().sendRemoteMessage(json.toString());
    }

    public static boolean copy(String fromFile, String toFile) {
        //要复制的文件目录
        File[] currentFiles;
        File root = new File(fromFile);
        //如同判断SD卡是否存在或者文件是否存在
        //如果不存在则 return出去
        if (!root.exists()) {
            return false;
        }
        //如果存在则获取当前目录下的全部文件 填充数组
        currentFiles = root.listFiles();

        //目标目录
        File targetDir = new File(toFile);
        //创建目录
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        //遍历要复制该目录下的全部文件
        for (int i = 0; i < currentFiles.length; i++) {
            if (currentFiles[i].isDirectory()) {
                //如果当前项为子目录 进行递归
                copy(currentFiles[i].getPath() + "/", toFile + currentFiles[i].getName() + "/");
            } else {
                //如果当前项为文件则进行文件拷贝
                if (currentFiles[i].getName().contains(".so")) {
                    int id = copySdcardFile(currentFiles[i].getPath(), toFile + File.separator + currentFiles[i].getName());
                }
            }
        }
        return true;
    }


    //文件拷贝
    //要复制的目录下的所有非子目录(文件夹)文件拷贝
    public static int copySdcardFile(String fromFile, String toFile) {

        try {
            FileInputStream fosfrom = new FileInputStream(fromFile);
            FileOutputStream fosto = new FileOutputStream(toFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = fosfrom.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            // 从内存到写入到具体文件
            fosto.write(baos.toByteArray());
            // 关闭文件流
            baos.close();
            fosto.close();
            fosfrom.close();
            return 0;

        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public static boolean isLoadSoFile(File dir) {
        File[] currentFiles;
        currentFiles = dir.listFiles();
        boolean hasJkffmpeg = false;
        if (currentFiles == null) {
            return false;
        }
        int count = 0;
        for (int i = 0; i < currentFiles.length; i++) {
            if (currentFiles[i].getName().contains("gnustl_shared") ||
                    currentFiles[i].getName().contains("oraycommon")||
                    currentFiles[i].getName().contains("orayservice_sdk")) {
                count ++;
            }
        }
        if (count == 3) {
            hasJkffmpeg = true;
        }
        return hasJkffmpeg;
    }

    public static boolean loadSoByName(Context context, String... args) {
        if (null == context) {
            return false;
        }
        File dir = context.getDir("lib", Context.MODE_PRIVATE);
        File[] currentFiles;
        if (null == dir) {
            return false;
        }
        currentFiles = dir.listFiles();
        if (null == currentFiles || currentFiles.length == 0) {
            return false;
        }
        boolean isLoadSuccess = true;
        int size = args.length;
        for (int j = 0; j < size; j ++) {
            for (int i = 0; i < currentFiles.length; i++) {
                if (currentFiles[i].getAbsolutePath().contains(args[j])) {
                    try {
                        System.load(currentFiles[i].getAbsolutePath());
                        isLoadSuccess = true;
                        break;
                    } catch (Throwable  e) {
                        return false;
                    }
                }
            }
        }
        return isLoadSuccess;
    }

    /**
     * Check if system libc.so is 32 bit or 64 bit
     */
    public static boolean isLibc64() {
        try {
            //32位so -- 能拉起来代表运行在32位cpu
            System.load("/system/lib/libc.so");
            return false;
        } catch (Throwable  e) {
            return true;
        }
//        File libcFile = new File("/system/lib/libc.so");
//        if (libcFile != null && libcFile.exists()) {
//            byte[] header = readELFHeadrIndentArray(libcFile);
//            if (header != null && header[4] == 2) {
//                return true;
//            }
//        }
//
//        File libcFile64 = new File("/system/lib64/libc.so");
//        if (libcFile64 != null && libcFile64.exists()) {
//            byte[] header = readELFHeadrIndentArray(libcFile64);
//            if (header != null && header[4] == 2) {
//                return true;
//            }
//        }

//        return false;
    }

    /**
     * ELF文件头格式是固定的:文件开始是一个16字节的byte数组e_indent[16]
     * e_indent[4]的值可以判断ELF是32位还是64位
     */
    private static byte[] readELFHeadrIndentArray(File libFile) {
        if (libFile != null && libFile.exists()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(libFile);
                if (inputStream != null) {
                    byte[] tempBuffer = new byte[16];
                    int count = inputStream.read(tempBuffer, 0, 16);
                    if (count == 16) {
                        return tempBuffer;
                    } else {
                        Log.e("readELFHeadrIndentArray", "Error: e_indent lenght should be 16, but actual is " +  count);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 解压，并复制到目标目录
     * @param zipFilePath
     * @param desFilePath
     * @param copyToPath
     */
    public static boolean unZipAndCopy(Context context, String zipFilePath, String desFilePath, String copyToPath) {
        if (null == context || TextUtils.isEmpty(zipFilePath) || TextUtils.isEmpty(desFilePath)) {
            return false;
        }
        File zipFile = new File(zipFilePath);
        String fileName = zipFile.getName();
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        File desFile = new File(desFilePath);
        if (!zipFile.exists()) {
            return false;
        }
        if (!desFile.exists()) {
            desFile.mkdirs();
        }
        if (UnZipFolder(context, zipFilePath, desFilePath)) {
            //解压成功
            return SoLoadUtil.copy(desFilePath + fileName + File.separator, copyToPath);
        }
        return false;
    }

    /**
     * DeCompress the ZIP to the path
     * @param zipFileString  name of ZIP
     * @param outPathString   path to be unZIP
     * @throws Exception
     */
    public static boolean UnZipFolder(Context context, String zipFileString, String outPathString) {
        try {
            ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
            ZipEntry zipEntry;
            String szName = "";
            while ((zipEntry = inZip.getNextEntry()) != null) {
                szName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    // get the folder name of the widget
                    szName = szName.substring(0, szName.length() - 1);
                    File folder = new File(outPathString + File.separator + szName);
                    folder.mkdirs();
                } else {

                    File file = new File(outPathString + File.separator + szName);
                    file.createNewFile();
                    // get the output stream of the file
                    FileOutputStream out = new FileOutputStream(file);
                    int len;
                    byte[] buffer = new byte[1024];
                    // read (len) bytes into buffer
                    while ((len = inZip.read(buffer)) != -1) {
                        // write (len) byte from buffer at the position 0
                        out.write(buffer, 0, len);
                        out.flush();
                    }
                    out.close();
                }
            }
            inZip.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断剩余空间是否足够
     * @param zipFileSize 下载的压缩文件大小
     * @param avachieSize 解压后的文件大小
     * @return
     */
    public static boolean isSdcardAvailble(long zipFileSize, long avachieSize) {
        long sd_freeSpace = Environment.getExternalStorageDirectory().getFreeSpace();
        if (sd_freeSpace > zipFileSize + avachieSize * 2) {
            return true;
        }
        return false;
    }
}
