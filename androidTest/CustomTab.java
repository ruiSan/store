package sdk.a71chat.com.demo;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.test.espresso.InjectEventSecurityException;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.action.MotionEvents;
import android.support.test.espresso.action.Tapper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by dell on 2017/12/15.
 */

public class CustomTab implements Tapper {

    private Handler mHandler;

    public CustomTab () {
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public Status sendTap(UiController uiController, float[] coordinates, float[] precision) {
        Tapper.Status stat = sendSingleTap(uiController, coordinates, precision);
        if (Tapper.Status.SUCCESS == stat) {
            // Wait until the touch event was processed by the main thread.
            long singlePressTimeout = (long) (ViewConfiguration.getTapTimeout() * 1.5f);
            uiController.loopMainThreadForAtLeast(singlePressTimeout);
        }
        return stat;
    }

    private Tapper.Status sendSingleTap(final UiController uiController,
                                        final float[] coordinates, final float[] precision) {
        long downTime = SystemClock.uptimeMillis();
        final MotionEvent motionEvent = MotionEvent.obtain(downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                coordinates[0],
                coordinates[1],
                0, // pressure
                1, // size
                0, // metaState
                precision[0], // xPrecision
                precision[1], // yPrecision
                0,  // deviceId
                0);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendUp(motionEvent, uiController, coordinates, precision);
            }
        }, 2000);
        try {
            uiController.injectMotionEvent(motionEvent);
        } catch (Exception e) {

        }
        return Tapper.Status.SUCCESS;
    }

    private void sendUp(MotionEvent downEvent, UiController uiController,
                        float[] coordinates, float[] precision) {
        MotionEvent motionEvent = null;
        motionEvent = MotionEvent.obtain(downEvent.getDownTime(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP,
                coordinates[0],
                coordinates[1],
                0);
        try {
            uiController.injectMotionEvent(motionEvent);
        } catch (Exception e) {

        }
    }
}
