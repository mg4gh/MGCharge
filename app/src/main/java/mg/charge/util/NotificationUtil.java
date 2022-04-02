package mg.charge.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;
import mg.charge.R;
import mg.charge.view.MGChargeActivity;

public class NotificationUtil {

    public static Notification createNotification(Context context){
        Notification notification;
        String CHANNEL_ID = "my_channel_01";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "MGMapViewer information channel",
                NotificationManager.IMPORTANCE_LOW);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        Log.i(MGChargeApplication.TAG, NameUtil.context()+" importance: "+channel.getImportance());

        Intent intent = new Intent(context, MGChargeActivity.class);
        notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mg2)
                .setContentTitle("MGCharge")
                .setContentText("MGCharge is monitoring.")
                .setContentIntent(    PendingIntent.getActivity(context, 0, intent , PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setSound(null)
                .build();
        return notification;
    }

    public static void showNotification(Context context, Notification notification){
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1111,notification);
    }
    public static void cancelAllNotification(Context context){
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    }
}
