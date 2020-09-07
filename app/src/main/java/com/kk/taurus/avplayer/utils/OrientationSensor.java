/*
 * Copyright 2017 jiajunhui<junhui_jia@163.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.kk.taurus.avplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class OrientationSensor {

    private final int MSG_SENSOR = 888;

    private OnOrientationListener onOrientationListener;

    // 是否是竖屏
    private boolean isPortrait = true;

    private SensorManager sm;
    private OrientationSensorListener listener;
    private Sensor sensor;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SENSOR:
                    int orientation = msg.arg1;
                    if (orientation > 45 && orientation < 135) {
                        if (isPortrait) {
                            //切换成横屏反向：ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            callbackOnLandScape(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                            isPortrait = false;
                        }
                    } else if (orientation > 135 && orientation < 225) {
                        if (!isPortrait) {
                            /*
                             * 切换成竖屏反向：ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT(9),
                             * ActivityInfo.SCREEN_ORIENTATION_SENSOR:根据重力感应自动旋转
                             * 此处正常应该是上面第一个属性，但是在真机测试时显示为竖屏正向，所以用第二个替代
                             */
                            callbackOnPortrait(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            isPortrait = true;
                        }
                    } else if (orientation > 225 && orientation < 315) {
                        if (isPortrait) {
                            //切换成横屏：ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            callbackOnLandScape(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            isPortrait = false;
                        }
                    } else if ((orientation > 315 && orientation < 360) || (orientation > 0 && orientation < 45)) {
                        if (!isPortrait) {
                            //切换成竖屏ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            callbackOnPortrait(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            isPortrait = true;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };


    public OrientationSensor(Activity activity, OnOrientationListener orientationListener){
        this.onOrientationListener = orientationListener;

        // 注册重力感应器,监听屏幕旋转
        sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        listener = new OrientationSensorListener(mHandler);
    }

    private void callbackOnLandScape(int orientation){
        if(onOrientationListener!=null)
            onOrientationListener.onLandScape(orientation);
    }

    private void callbackOnPortrait(int orientation){
        if(onOrientationListener!=null)
            onOrientationListener.onPortrait(orientation);
    }

    public void enable(){
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void disable(){
        sm.unregisterListener(listener);
    }

    public boolean isPortrait(){
        return this.isPortrait;
    }

    /**
     * 重力感应监听者
     */
    public class OrientationSensorListener implements SensorEventListener {
        private static final int _DATA_X = 0;
        private static final int _DATA_Y = 1;
        private static final int _DATA_Z = 2;

        public static final int ORIENTATION_UNKNOWN = -1;

        private Handler rotateHandler;

        public OrientationSensorListener(Handler handler) {
            rotateHandler = handler;
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            int orientation = ORIENTATION_UNKNOWN;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];
            float magnitude = X * X + Y * Y;
            // Don't trust the angle if the magnitude is small compared to the y
            // value
            if (magnitude * 4 >= Z * Z) {
                // 屏幕旋转时
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                orientation = 90 - (int) Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }
            if (rotateHandler != null) {
                rotateHandler.obtainMessage(MSG_SENSOR, orientation, 0).sendToTarget();
            }
        }
    }

    public interface OnOrientationListener{
        void onLandScape(int orientation);
        void onPortrait(int orientation);
    }

}
