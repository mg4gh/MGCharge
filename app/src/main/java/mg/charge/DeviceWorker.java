package mg.charge;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import mg.charge.control.DeviceActionGetStatus;
import mg.charge.control.DeviceActionTurn;
import mg.charge.model.DeviceStatus;
import mg.charge.view.MGChargeActivity;

public class DeviceWorker extends Worker {

    public DeviceWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @androidx.annotation.NonNull
    @Override
    public Result doWork() {
        try {
            Log.i(MGChargeApplication.TAG, NameUtil.context()+" doWork!!!!");
            MGChargeApplication application = (MGChargeApplication) getApplicationContext();

            DeviceStatus deviceStatus = new DeviceActionGetStatus(application.getSelectedDevice()).execute();
            long[] retriggerTime = new long[]{1000};
            boolean shouldBeOn = application.getTargetState(deviceStatus.isOn(), System.currentTimeMillis(), retriggerTime);
            boolean inactive = false;
            if (shouldBeOn != deviceStatus.isOn()){
                new DeviceActionTurn(application.getSelectedDevice(), shouldBeOn).execute();
                deviceStatus = new DeviceActionGetStatus(application.getSelectedDevice()).execute();
                inactive = !shouldBeOn;
            }
            application.setDeviceStatus(deviceStatus);
            if (inactive){
                application.setActive(false);
                if (application.activityCount == 0){
                    Log.i(MGChargeApplication.TAG, NameUtil.context()+" trigger MGChargeApplication to exit");
                    Intent intent = new Intent(getApplicationContext(), MGChargeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("EXIT", true);
                    intent.putExtra("ACTIVITY_COUNT", application.activityCount);
                    getApplicationContext().startActivity(intent);
                    new Handler(Looper.getMainLooper()).postDelayed(()->System.exit(0),2*1000);
                }
            } else {
                long retrigger = (retriggerTime[0]<0)?60:Math.max(1,Math.min(retriggerTime[0], 60));
                Log.i(MGChargeApplication.TAG, NameUtil.context()+" retrigger in "+retrigger);
                OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DeviceWorker.class)
                        .setInitialDelay(retrigger, TimeUnit.SECONDS).build();
                String uniqueWokName = getApplicationContext().getString(R.string.unique_work_name);
                WorkManager.getInstance(application).enqueueUniqueWork(uniqueWokName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success();
    }

}
