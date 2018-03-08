package sdk.a71chat.com.demo.Download;

import java.util.List;

/**
 * Created by dell on 2018/3/6.
 * 下载管理器 -- 本类做成单列
 */
public class DownLoadManager {

    private List<DownLoadTask> downLoadTaskList;

    public static DownLoadManager getInstance() {
        return DownLoadImpl.downLoadManager;
    }

    /**
     * 将任务添加到下载列表 -- 若下载列表为空，执行下载 -- 不为空，等待执行
     * @param downLoadTask
     */
    public synchronized void addTask(DownLoadTask downLoadTask) {
        if (null == downLoadTaskList || downLoadTaskList.size() == 0) {
            downLoadTaskList.add(downLoadTask);
            downLoadTask.taskRun();
        }
    }

    /**
     * 下载任务完成，移除task -- 执行下一个任务
     */
    public synchronized void removeTask(DownLoadTask downLoadTask) {
        if (null == downLoadTaskList || downLoadTaskList.size() == 0) {
            return;
        }

        if (downLoadTaskList.contains(downLoadTask)) {
            downLoadTaskList.remove(downLoadTask);
        }

        if (downLoadTaskList.size() != 0) {
            downLoadTaskList.get(0).taskRun();
        }
    }

    public static class DownLoadImpl {
        public static DownLoadManager downLoadManager = new DownLoadManager();
    }

}
