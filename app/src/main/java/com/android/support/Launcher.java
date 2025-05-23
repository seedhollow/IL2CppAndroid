package com.android.support;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class Launcher extends Service {

    Menu menu;

    //When this Class is called the code in this function will be executed
    @Override
    public void onCreate() {
        super.onCreate();
        menu = new Menu(this);
    }

    private void startMenu() {
        if (menu != null) {
            menu.SetWindowManagerWindowService();
            menu.ShowMenu();

            final Handler handler = new Handler();
            handler.post(new Runnable() {
                public void run() {
                    UpdateMenuVisibility();
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    private void pollForPermissionGrant(Context context) {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            private int attempts = 0;

            @Override
            public void run() {
                if (Utils.CheckOverlayPermissions(context)) {
                    if (BuildConfig.DEBUG) Log.d(Menu.TAG, "Permission Granted Starting Menu");
                    startMenu();
                } else if (attempts < 10) {
                    if (BuildConfig.DEBUG) Log.d(Menu.TAG, "Permission Not Granted,Attempt: " + attempts + " Checking Again");
                    attempts++;
                    handler.postDelayed(this, 1000);
                } else {
                    if (BuildConfig.DEBUG) Log.d(Menu.TAG, "Permission Not Granted,Attempt: " + attempts + " Stopping service");
                    stopSelf();
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Check if we are still in the game. If now our menu and menu button will dissapear
    private boolean isNotInGame() {
        ActivityManager.RunningAppProcessInfo runningAppProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(runningAppProcessInfo);
        return runningAppProcessInfo.importance != 100;
    }

    private void UpdateMenuVisibility() {
        menu.setModMenuVisibility(!isNotInGame());
        menu.setEspLayoutVisibility(!isNotInGame());
    }

    //Destroy our View
    @Override
    public void onDestroy() {
        super.onDestroy();
        menu.onDestroy();
    }

    //Same as above so it wont crash in the background and therefore use alot of Battery life
    @Override
    public void onTaskRemoved(Intent intent) {
        super.onTaskRemoved(intent);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopSelf();
    }

    //Override our Start Command so the Service doesnt try to recreate itself when the App is closed
    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent != null) {
            if (intent.getBooleanExtra(Main.OVERLAY_PERMISSION_KEY, true)) {
                startMenu();
            } else {
                pollForPermissionGrant(this);
            }
        }
        return Service.START_NOT_STICKY;
    }
}
