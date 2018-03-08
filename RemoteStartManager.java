package com.sevenonechat.sdk.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;

import com.alibaba.fastjson.JSONObject;
import com.sevenonechat.client.ClientImpl;
import com.sevenonechat.sdk.permission.PermissionUtils;
import com.sevenonechat.sdk.sdkinfo.SdkRunningClient;
import com.sevenonechat.sdk.service.Constants;
import com.sevenonechat.sdk.thirdParty.eventbus.EventBus;
import com.sevenonechat.sdk.util.Download.DownLoadManager;
import com.sevenonechat.sdk.util.Download.DownLoadTask;
import com.sevenonechat.sdk.util.Download.FileInfo;

import java.io.File;

/**
 * Created by dell on 2018/2/5.
 * 远程协助调用类
 * //1.检测是否含推送代码
 *  //2.判断是否已加载到内存
 * // 3.未加载，判断是否含有足够的空间
 * //4.检测申请权限
 * //5. 判断是否已下载
 * 6.未下载执行下载
 */
public class RemoteStartManager implements ClientImpl.ClientImplInterface {

    private Activity activity;

    private Handler mHandler;

    private Dialog dialog;

    /**
     * 由访客端发起远程会话，客服确认的弹出框
     */
    private Dialog comfirmDialog;

    /**
     * 当前设备是否正处于远程协助中
     */
    private volatile boolean isInRemoting = false;

    /**
     *  客服端是否取消远程协助
     */
    private volatile boolean isCustomerCancle = false;

    /**
     * 0代表访客主动发起远程，1代表客服主动发起远程
     */
    private int currentType = 0;

    private String currentSessionId = "";

    private ClientImpl clientImpl;

    public static RemoteStartManager getInstance() {
        return UserInforCacheSingleton.client;
    }

    public RemoteStartManager() {
        mHandler = new Handler();
    }

    public void setActivity(Activity context) {
        if (null == activity) {
            activity = context;
        }
        if (null == clientImpl) {
            clientImpl = new ClientImpl(context, this);
        } else {
            clientImpl.setActivity(context);
        }
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(String currentSessionId) {
        this.currentSessionId = currentSessionId;
    }

    public void stopRemote() {
        if (isInRemoting) {
            SoLoadUtil.sendRemoteRequest(6);
            isInRemoting = false;
        }
        currentSessionId = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!isInRemoting) {
            return;
        }
        if (null != clientImpl) {
            clientImpl.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void start(int type) {
        if (null == activity || isInRemoting) {
            return;
        }
        if (isInRemoting) {
            SoLoadUtil.sendRemoteRequest(3);
        }
        currentType = type;
        if (type == 0) {
            isCustomerCancle = false;
        }
        isInRemoting = true;
        if (SoLoadUtil.isLoadSoFile(ContextHolder.get().getContext().getDir("lib", Context.MODE_PRIVATE))) {
            loadRemoteSo();
            return;
        }
        processPermission(activity);
    }

    /**
     * 处理权限
     */
    public void processPermission(Activity context) {
        int writepermission = ActivityCompat.checkSelfPermission(context, PermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(context, PermissionUtils.PERMISSION_READ_EXTERNAL_STORAGE);
        if (PackageManager.PERMISSION_GRANTED == writepermission && PackageManager.PERMISSION_GRANTED == readPermission) {
            copyAndLibrary();
        } else {
            if (PackageManager.PERMISSION_GRANTED != readPermission) {
                PermissionUtils.requestPermission(context, PermissionUtils.CODE_READ_EXTERNAL_STORAGE, mPermissionGrant);
            }
        }
    }

    private PermissionUtils.PermissionGrant mPermissionGrant = new PermissionUtils.PermissionGrant() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case PermissionUtils.CODE_READ_EXTERNAL_STORAGE:
                    int writepermission = ActivityCompat.checkSelfPermission(activity, PermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE);
                    if (PackageManager.PERMISSION_GRANTED == writepermission) {
                        copyAndLibrary();
                    } else {
                        PermissionUtils.requestPermission(activity, PermissionUtils.CODE_READ_EXTERNAL_STORAGE, mPermissionGrant);
                    }
                    break;
                case PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE:
                    copyAndLibrary();
                    break;
                default:
                    break;
            }
        }
    };

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!isInRemoting) {
            return;
        }
        if (null == activity) {
            sendCannotUse();
            return;
        }
        if (requestCode == PermissionUtils.CODE_READ_EXTERNAL_STORAGE && grantResults[0] == 0) {
            int writepermission = ActivityCompat.checkSelfPermission(activity, PermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE);
            if (PackageManager.PERMISSION_GRANTED == writepermission) {
                copyAndLibrary();
            } else {
                PermissionUtils.requestPermission(activity, PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE, mPermissionGrant);
            }
        } else if (requestCode == PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE && grantResults[0] == 0) {
            copyAndLibrary();
        } else if (requestCode == PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE || requestCode == PermissionUtils.CODE_READ_EXTERNAL_STORAGE) {
            sendCannotUse();
        }
    }

    private void sendCannotUse() {
        if (currentType == 1 && isInRemoting) {
            SoLoadUtil.sendRemoteRequest(3);
        }
        isInRemoting = false;
    }

    /**
     * 检测so库文件是否存在，若不存在，从下载目录复制进入
     */
    private void copyAndLibrary() {
        //实际是根据CPU从后台下载对应的so库,下载完成后执行解压复制
        if (SoLoadUtil.isLibc64()) { //64位系统
            startDownLoad(64);
        } else { //32位系统
            startDownLoad(32);
        }
    }

    private void startDownLoad(int cpuType) {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator
                + "client/armeabi.zip");
        if (file.exists()) {
            processCopy();
            return;
        }
        File file1 = new File(Environment.getExternalStorageDirectory() + File.separator
                + "client");
        if (!file1.exists()) {
            file1.mkdirs();
        }
        String url = Constants.API_SERVER;
//        String url = "https://test.kefu.com/scsf/";
        if (32 != cpuType) {
            url = url + "core/common.download.do?url=https://ftp.71chat.com/public/remoter/android-remoter-clientArm32.zip&";
        } else {
            url = url + "core/common.download.do?url=https://ftp.71chat.com/public/remoter/android-remoter-clientArm64.zip&";
        }
        String filename = "fileName=armeabi.zip";
//        DownLoadManager.getInstance().addTask(new DownLoadTask(new FileInfo(url + filename, "armeabi.zip")));
        DownloadFileAsync downloadFileAsync = new DownloadFileAsync(activity);
        downloadFileAsync.setDownLoadInter(new DownloadFileAsync.DownLoadInter() {
            @Override
            public void downLoadSuccess() {
                processCopy();
            }

            @Override
            public void downLoadFail() {
                sendCannotUse();
            }
        });
        downloadFileAsync.execute(url + filename, Environment.getExternalStorageDirectory() + File.separator
                + "client/armeabi.zip");
    }

    private void processCopy() {
        if (null == activity) {
            sendCannotUse();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (SoLoadUtil.unZipAndCopy(activity, Environment.getExternalStorageDirectory() + File.separator
                        + "client/armeabi.zip", Environment.getExternalStorageDirectory() + File.separator
                        + "client/lib/", ContextHolder.get().getContext().getDir("lib", Context.MODE_PRIVATE).getPath())) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != activity) {
                                ToastUtil.showTips(activity, "库文件解压，复制成功");
                                loadRemoteSo();
                            }
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sendCannotUse();
                            if (null != activity) {
                                ToastUtil.showTips(activity, "库文件解压，复制失败");
                            }
                        }
                    });
                }
            }
        }).start();

    }

    /**
     * 动态加载远程协助so库
     */
    private void loadRemoteSo() {
        if (null == activity) {
            sendCannotUse();
            return;
        }
        if (isCustomerCancle) {
            isCustomerCancle = false;
            isInRemoting = false;
            return;
        }
        String[] soNames = new String[]{"gnustl_shared", "oraycommon", "orayservice_sdk"};
        if (SdkRunningClient.getInstance().isLoadRemoteSo()) {
            clientImpl.startSdkLogin();
        } else {
            if (SoLoadUtil.loadSoByName(activity, soNames)) {
                SdkRunningClient.getInstance().setLoadRemoteSo(true);
                //执行登录
                clientImpl.startSdkLogin();
            } else {//so库下载错误，重新执行下载流程
                sendCannotUse();
                if (null != activity) {
                    ToastUtil.showTips(activity, "远程协助so库加载失败，请确认下载的so库正确");
                }
            }
        }
    }

    /**
     *  处理远程消息
     */
    public void processMessage(String wds) {
        try {
            JSONObject json = JsonHelper.parseObject(wds);
            String flag = json.getString("flag");
            if (flag.equals("10")) {
                //客户端远程协助控件不可用
                isInRemoting = false;
                dismissDialog();
                ToastUtil.showTips(activity, "客服远程协助控件不可用");
            } else if (flag.equals("9")) {
                isInRemoting = false;
                dismissDialog();
                ToastUtil.showTips(activity, "客服拒绝了本次远程协助请求");
            } else if (flag.equals("8")) {
                //客服同意
                dismissDialog();
                EventBus.getDefault().post("connectedRemote");
            } else if (flag.equals("0")) {
                //客服主动发起远程协助
                if (!TextUtils.isEmpty(Constants.getRoomId())) {
                    currentSessionId = Constants.getRoomId();
                }
                showComfirmDialog();
            } else if (flag.equals("6")) {
                dismissDialog();
                dismissComfirmDialog();
                isCustomerCancle = false;
                isInRemoting = false;
            }
        } catch (Exception e) {

        }
    }

    /**
     * 显示确认弹出框
     */
    private void showComfirmDialog() {
        comfirmDialog = DialogUtil.createClientRequestDialog(activity, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isOk = (boolean) v.getTag();
                if (isOk) { //同意远程请求
                    EventBus.getDefault().post("connectedRemote");
                    start(1);
                } else { //拒绝
                    isInRemoting = false;
                    SoLoadUtil.sendRemoteRequest(2);
                }
            }
        });
        comfirmDialog.show();
    }

    /**
     * 销毁弹出框
     */
    private void dismissComfirmDialog() {
        try {
            if (null != comfirmDialog && comfirmDialog.isShowing()) {
                comfirmDialog.dismiss();
            }
        } catch (Exception e) {

        }
    }

    /**
     * 显示等待远程弹出框
     */
    private void showWaitingDialog() {
        try {
            if (null == dialog) {
                dialog = DialogUtil.createRemoteDialog(activity, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //取消
                        isInRemoting = false;
                        SoLoadUtil.sendRemoteRequest(6);
                    }
                });
            }
            dialog.show();
        } catch (Exception e) {

        }
    }

    /**
     * 销毁弹出框
     */
    private void dismissDialog() {
        try {
            if (null != dialog && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {

        }
    }

    /**
     * 是否正在远程控制中
     * @return
     */
    public boolean isInRemoting() {
        return isInRemoting;
    }

    public void release() {
        activity = null;
        dismissDialog();
        dismissComfirmDialog();
        dialog = null;
        comfirmDialog = null;
        if (null != clientImpl) {
            clientImpl.release();
        }
    }

    @Override
    public void sendFlags(int flag) {

    }

    @Override
    public void showTips(String tips) {

    }

    @Override
    public void processStatusChange(int status) {
        switch (status) {
            case 3:
                break;
            case 4:
                ToastUtil.showTips(activity, "远程断开连接");
                break;
            case 1:
                clientImpl.createSession();
                ToastUtil.showTips(activity, "远程登录成功");
                break;
            case 2:
                ToastUtil.showTips(activity, "远程连接登录失败，请检查网络");
                sendCannotUse();
                break;
            case 5:

                ToastUtil.showTips(activity, "远程连接成功");
                break;
            case 6:
                EventBus.getDefault().post("stopRemote");
                ToastUtil.showTips(activity, "远程连接断开");
                isInRemoting = false;
                break;
            case 21:
                sendCannotUse();
                ToastUtil.showTips(activity, "无效的参数");
                break;
            case 22:
                sendCannotUse();
                ToastUtil.showTips(activity, "无效的授权");
                break;
            case 24:
                sendCannotUse();
                ToastUtil.showTips(activity, "无效的协议");
                break;
            case 23:
                sendCannotUse();
                ToastUtil.showTips(activity, "无效的地址");
                break;
            case 25:
                sendCannotUse();
                ToastUtil.showTips(activity, "授权已经过期");
                break;
            case 27:
                sendCannotUse();
                ToastUtil.showTips(activity, "登录失败");
                break;
            case 26:
                sendCannotUse();
                ToastUtil.showTips(activity, "appid/appkey验证失败");
                break;
            default:
                sendCannotUse();
                ToastUtil.showTips(activity, "未知错误，请确认网络或appid与appkey");
        }
    }

    @Override
    public void createSessionAfter(String address, String session) {
        if (currentType == 0) {
            SoLoadUtil.sendClientRequest(7, address, session);
            showWaitingDialog();
        } else {
            SoLoadUtil.sendClientRequest(1, address, session);
        }
    }

    private static class UserInforCacheSingleton {
        private static RemoteStartManager client = new RemoteStartManager();
    }

}
