package mg.charge;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

    SharedPreferences preferences;

    public DeviceWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @androidx.annotation.NonNull
    @Override
    public Result doWork() {
        try {
            Log.i(MGChargeApplication.TAG, NameUtil.context()+" doWork!!!!");
            MGChargeApplication application = (MGChargeApplication) getApplicationContext();

            DeviceStatus deviceStatus = new DeviceActionGetStatus(application.getSelectedDevice()).execute();
            boolean shouldBeOn = application.getTargetState(deviceStatus.isOn(), System.currentTimeMillis());
            boolean exit = false;
            if (shouldBeOn != deviceStatus.isOn()){
                new DeviceActionTurn(application.getSelectedDevice(), shouldBeOn).execute();
                deviceStatus = new DeviceActionGetStatus(application.getSelectedDevice()).execute();
                exit = !shouldBeOn;
            }
            application.setDeviceStatus(deviceStatus);
            if (exit){
                Log.i(MGChargeApplication.TAG, NameUtil.context()+" trigger MGChargeActivity to exit");
                Intent intent = new Intent(getApplicationContext(), MGChargeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("EXIT", true);
                intent.putExtra("ACTIVITY_COUNT", application.activityCount);
                getApplicationContext().startActivity(intent);
            } else {
                OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DeviceWorker.class)
                        .setInitialDelay(60, TimeUnit.SECONDS).build();
                String uniqueWokName = getApplicationContext().getString(R.string.unique_work_name);
                WorkManager.getInstance().enqueueUniqueWork(uniqueWokName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success();
    }




};
