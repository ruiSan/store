package sdk.a71chat.com.demo;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import com.sevenonechat.sdk.chatview.widgets.emotion.data.Emoticon;
import com.sevenonechat.sdk.sdkCallBack.DelayType;
import com.sevenonechat.sdk.sdkinfo.SdkRunningClient;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.core.deps.guava.collect.Iterables.getOnlyElement;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Created by dell on 2017/12/11.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    private EspressoTestImpl mEs;

    private Handler mHandler;

    private volatile Activity currentActivity;

    private Object objLock;

    @Before
    public void registerIdlingResource() {
        // To prove that the test fails, omit this call:
        mHandler = new Handler(Looper.getMainLooper());
        objLock = new Object();
        Espresso.registerIdlingResources(DemoApp.mIdlingResource);
        mEs = new EspressoTestImpl();
        SdkRunningClient.getInstance().setTestInterface(mEs);
    }

    @Test
    public void startChat() {
        IdlingPolicies.setMasterPolicyTimeout(
                1000 * 60, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(
                1000 * 60, TimeUnit.MILLISECONDS);
//        for (int i = 0; i < 10; i ++) {
            testAround();
//        }
    }

    private void testAround() {
        //进入聊天界面
        mEs.clearMap();
        checkIfViewVisible(R.id.startBtn, TextView.class);
        onView(withText("联系客服")).perform(click());
        switch (mEs.getType()) {
            case TYPE_CHAT:
                testTypeChat();
                break;
            case TYPE_VISITOR:
                testVisitorFrag(false);
                break;
            case TYPE_LEAVE_MSG:
                testLeaveMsgFrag();
                break;
            case TYPE_FAILED:
                break;
        }
    }

    /**
     * 聊天界面的测试
     */
    private void testTypeChat() {
        onView(withId(R.id.id_edit)).perform(typeText("aaarrrrr"), closeSoftKeyboard());
        onView(withText("发送")).perform(click());
        //等待
        if (SdkRunningClient.getInstance().isRobotStatus()) {
            SdkRunningClient.getInstance().setEspressoTestIdle(null, 2, false);
            onView(withId(R.id.iv_to_customer)).perform(click());
            switch (mEs.getType()) {
                case TYPE_CHAT:
                    testChatProcess();
                    break;
                case TYPE_VISITOR:
                    testVisitorFrag(true);
                    break;
                case TYPE_LEAVE_MSG:
                    testLeaveMsgFrag();
                    break;
                case TYPE_FAILED:
                    break;
            }
        } else {
            //不是机器人状态，直接测试聊天界面
            testChatProcess();
        }
    }

    /**
     * 处于转人工状态 -- 消息界面处理
     */
    private void testChatProcess() {
        testSendText("智八哥，你好");

        //模拟发送语音消息
        onView(withId(R.id.voice_button)).perform(click());
        ViewInteraction talkIn = onView(withId(R.id.press_to_talk));
        talkIn.perform(ViewActions.actionWithAssertions
                (new GeneralClickAction(new CustomTab(), GeneralLocation.VISIBLE_CENTER, Press.FINGER)));
        onView(withId(R.id.voice2chat_button)).perform(click());

        testSendEmoji(1, true);
        testSendText("今天天气怎么样");
        testSendEmoji(3, false);
        testSendText("机器人几岁了");
        testSendEmoji(6, false);
        testSendText("good day");
        testSendImge();
        //关闭会话
        onView(withId(R.id.tv_right)).perform(click());

        if (SdkRunningClient.getInstance().isEvaluteSended()) {
            onView(withId(R.id.tv_notific_confirm)).perform(click());
        } else {
            if (!SdkRunningClient.getInstance().isShowEvaluteDialog()) {
                delayPressBack();
            } else {
                onView(withId(R.id.btn_right)).perform(click());
            }
        }
    }

    /**
     * 测试发送文字
     */
    private void testSendText(String text) {
        try {
            onView(withId(R.id.id_edit)).perform(typeText(text), closeSoftKeyboard());
        } catch (Exception e) {
            onView(withId(R.id.id_edit)).perform(replaceText(text), closeSoftKeyboard());
        }
        onView(withText("发送")).perform(click());
    }

    /**
     * 测试发送图片
     */
    private void testSendImge() {
        onView(withId(R.id.send2tool_button)).perform(click());
//        onView(withId(R.id.folder_list)).perform(RecyclerViewActions.actionOnItemAtPosition(1,click()));
        onView(withId(R.id.folder_list)).perform(RecyclerViewActions.actionOnItemAtPosition(1,click()));
        onView(withId(R.id.checkbox_select)).perform(click());
        onView(withId(R.id.done_text)).perform(click());
        SdkRunningClient.getInstance().setEspressoTestIdle(null, 6, false);
    }

    /**
     * 测试发送表情
     */
    private void testSendEmoji(int position, boolean isFirst) {
        onView(withId(R.id.voice_button)).perform(click(), closeSoftKeyboard());
        onView(withId(R.id.voice2chat_button)).perform(click(), closeSoftKeyboard());

        onView(withId(R.id.emoji_button)).perform(click(), closeSoftKeyboard());
        //发送表情
        SdkRunningClient.getInstance().setEspressoTestIdle(DelayType.DELAY_TYPE, 100, false);
        SdkRunningClient.getInstance().setEspressoTestIdle(DelayType.DELAY_TYPE, 100, true);
        onData(instanceOf(Emoticon.class)).
                inAdapterView(allOf(instanceOf(GridView.class), isDisplayed())).
                atPosition(position).perform(click());
        SdkRunningClient.getInstance().setEspressoTestIdle(DelayType.DELAY_TYPE, 100, false);
        SdkRunningClient.getInstance().setEspressoTestIdle(DelayType.DELAY_TYPE, 100, true);
        onView(withText("发送")).perform(click());
    }

    /**
     * 测试转人工界面
     */
    private void testVisitorFrag(boolean isHasRobot) {
        if (null != mEs.getNetType()) {
            switch (mEs.getNetType()) {
                case REQ_SUCCESS:
                    testVisitorAndChat(isHasRobot);
                    break;
                case REQ_FAILED:
                    delayPressBack();
                    break;
            }
        } else {
            testVisitorAndChat(isHasRobot);
        }
    }

    private void testVisitorAndChat(boolean isHasRobot) {
        visitorInput();
        onView(withId(R.id.btn_back)).perform(click());
        if (isHasRobot) {
            onView(withId(R.id.iv_to_customer)).perform(click());
        } else {
            onView(withText("联系客服")).perform(click());
        }
        visitorInput();
        onView(withId(R.id.btn_submit)).perform(click());
        testChatProcess();
    }

    /**
     * 选择主题界面 输入
     */
    private void visitorInput() {
        if (checkIfViewVisible(R.id.et_name, EditText.class)) {
            onView(withId(R.id.et_name)).perform(replaceText("刘大哥"));
        }
        if (checkIfViewVisible(R.id.et_email, EditText.class)) {
            onView(withId(R.id.et_email)).perform(replaceText("349898184@qq.com"));
        }
        if (checkIfViewVisible(R.id.et_mobile, EditText.class)) {
            onView(withId(R.id.et_mobile)).perform(replaceText("15919877417"));
        }
        if (checkIfViewVisible(R.id.et_qq, EditText.class)) {
            onView(withId(R.id.et_qq)).perform(replaceText("123456"));
        }
    }

    /**
     * 留言界面输入
     */
    private void leaveInput() {
        if (checkIfViewVisible(R.id.leave_msg_content, EditText.class)) {
            onView(withId(R.id.leave_msg_content)).perform(replaceText("你们怎么没人在线啊"));
        }
        if (checkIfViewVisible(R.id.leave_msg_email, EditText.class)) {
            onView(withId(R.id.leave_msg_email)).perform(replaceText("349898184@qq.com"));
        }
        if (checkIfViewVisible(R.id.leave_msg_phone, EditText.class)) {
            onView(withId(R.id.leave_msg_phone)).perform(replaceText("15105191127"));
        }
        if (checkIfViewVisible(R.id.leave_msg_name, EditText.class)) {
            onView(withId(R.id.leave_msg_name)).perform(replaceText("刘大哥"));
        }
    }

    /**
     * 留言界面测试
     */
    private void testLeaveMsgFrag() {
        switch (mEs.getNetType()) {
            case REQ_SUCCESS:
                leaveInput();
                onView(withId(R.id.leaveSubmit)).perform(click());
                if (null == mEs.getNetType()) { //留言成功
                    if (!isTheFirstActivity()) {
                        delayPressBack();
                    }
                } else {
                    //留言请求服务失败
                    delayPressBack();
                    delayPressBack();
                }
                break;
            case REQ_FAILED:
                delayPressBack();
                break;
        }
    }

    private void delayPressBack() {
        pressBack();
        SdkRunningClient.getInstance().setEspressoTestIdle(DelayType.DELAY_TYPE, 100, false);
        SdkRunningClient.getInstance().setEspressoTestIdle(DelayType.DELAY_TYPE, 100, true);
    }

    @After
    public void unregisterIdlingResource() {
        if (DemoApp.mIdlingResource != null) {
            Espresso.unregisterIdlingResources(DemoApp.mIdlingResource);
        }
    }

    /**
     * 判断控件是否在界面上显示
     * @param resId
     * @param cls
     * @return
     */
    public boolean checkIfViewVisible(int resId, Class cls) {
        ViewVisibleAction visibleAction = new ViewVisibleAction(cls, resId);
        onView(withId(resId)).perform(ViewActions.actionWithAssertions(visibleAction));
        if (visibleAction.isViewVisible()) {
            return true;
        }
        return false;
    }

    public class ViewVisibleAction implements ViewAction {
        private View view;
        private Class aClass;
        private int resId;
        public ViewVisibleAction(Class cls, int id) {
            aClass = cls;
            resId = id;
        }
        @Override
        public Matcher<View> getConstraints() {
            //由于是用于判断控件是否存在，故这里故意返回相同的Matcher
            return withId(resId);
        }
        @Override
        public String getDescription() {
            return "check view visible";
        }
        @Override
        public void perform(UiController uiController, View view) {
            this.view = view;
        }
        public boolean isViewVisible() {
            //判断界面上显示的view是否包含 需要校验的控件,包含，则控件为visible，否则为Gone
            if (allOf(isDisplayed(), isAssignableFrom(aClass)).matches(view)) {
                return true;
            }
            return false;
        }
    }

    /**
     * 获取当前所在activity
     * @return
     */
    private Activity getCurrentActivity() {
        Collection<Activity> resumedActivities =
                ActivityLifecycleMonitorRegistry.getInstance().
                        getActivitiesInStage(Stage.RESUMED);
        return getOnlyElement(resumedActivities);
    }

    private boolean isTheFirstActivity() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                currentActivity = getCurrentActivity();
                synchronized (objLock) {
                    objLock.notifyAll();
                }
            }
        });
        synchronized (objLock) {
            try {
                objLock.wait();
            } catch (Exception e) {

            }

        }
        if (currentActivity instanceof MainActivity) {
            return true;
        }
        return false;
    }
}