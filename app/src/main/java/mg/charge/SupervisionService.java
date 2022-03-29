package mg.charge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SupervisionService extends Service {

    private Notification notification = null;
    MGChargeApplication mgChargeApplication;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mgChargeApplication = (MGChargeApplication)getApplication();


        String CHANNEL_ID = "my_channel_01";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "MGMapViewer information channel",
                NotificationManager.IMPORTANCE_LOW);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        Log.i(MGChargeApplication.TAG, NameUtil.context()+" importance: "+channel.getImportance());

        Intent intent = new Intent(getApplicationContext(), MGChargeActivity.class);
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.mg2)
                .setContentTitle("MGCharge")
                .setContentText("MGCharge is monitoring.")
                .setContentIntent(    PendingIntent.getActivity(this.getApplicationContext(), 0, intent , PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setSound(null)
                .build();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(MGChargeApplication.TAG, NameUtil.context()+" superviseDevice="+mgChargeApplication.superviseDevice+" shouldSuperviseDevice="+mgChargeApplication.shouldSuperviseDevice);
        if (!mgChargeApplication.superviseDevice && mgChargeApplication.shouldSuperviseDevice){
            activateService();
            mgChargeApplication.setSuperviseDevice(true);
        } else if (mgChargeApplication.superviseDevice && !mgChargeApplication.shouldSuperviseDevice){
            deactivateService();
            mgChargeApplication.setSuperviseDevice(false);
        }
        return START_STICKY;
    }

    void activateService(){
        startForeground(1, notification);

        new Thread(() -> {
            long lastAppWakeup = 0;
            while ( true ){
                try {
                    synchronized (SupervisionService.this){
                        SupervisionService.this.wait(20*1000); // internal 20s timeout
                    }
                    long now = System.currentTimeMillis();
                    Log.i(MGChargeApplication.TAG, NameUtil.context() + "waked up");

                    if (mgChargeApplication.isShouldSuperviseDevice()){
                        if (now - lastAppWakeup > 60*1000){
                            lastAppWakeup = now;
                            Log.i(MGChargeApplication.TAG, NameUtil.context() + "trigger wakeup app.");
                            mgChargeApplication.wakeupDeviceHandler();
                        }
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    Log.e(MGChargeApplication.TAG, NameUtil.context(), e);
                }
            }
            Log.i(MGChargeApplication.TAG, NameUtil.context()+" terminate supervision Thread.");
        }).start();
    }

    void deactivateService(){
        try {
            stopForeground(true);
            Thread.sleep(200);
            synchronized (this){
                this.notifyAll();
            }
        } catch (Exception e) {
            Log.e(MGChargeApplication.TAG, NameUtil.context(), e);
        }
        Log.i(MGChargeApplication.TAG, NameUtil.context() +" deactivate done.");
    }

    @Override
    public void onDestroy() {
        if (mgChargeApplication.superviseDevice){
            deactivateService();
        }
        super.onDestroy();
    }
}
