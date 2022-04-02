package mg.charge;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import mg.charge.model.Device;
import mg.charge.model.DeviceStatus;
import mg.charge.util.NotificationUtil;

public class MGChargeApplication extends Application {

    public static final String TAG = "MGCharge";

    SharedPreferences preferences;

    ArrayList<Device> devices = new ArrayList<>();
    Device selectedDevice = null;
    MGObservable appObservable = new MGObservable();
    int activityCount = 0;
    DeviceStatus deviceStatus = new DeviceStatus();

    File path;
    File logDir = null;
    private Process pLogcat = null;
    Notification notification = null;

    /** indicates whether application is active (not active after auto switch off, before app is opened again) */
    boolean active = false;


    public ArrayList<Device> getDevices() {
        return devices;
    }

    public MGObservable getAppObservable() {
        return appObservable;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void actvityCountUpdate(boolean resume){
        activityCount += resume?1:-1;
        Log.i(TAG, NameUtil.context()+" activityCount="+activityCount);
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
        appObservable.setChanged();
        appObservable.notifyObservers();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active){
            Log.i(TAG, NameUtil.context()+" set active="+active);
            this.active = active;
            if (active){
                OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DeviceWorker.class)
                        .setInitialDelay(60, TimeUnit.SECONDS).build();
                String uniqueWokName = getApplicationContext().getString(R.string.unique_work_name);
                Operation res = WorkManager.getInstance().enqueueUniqueWork(uniqueWokName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);

                NotificationUtil.showNotification(this, notification);
                Log.i(TAG, NameUtil.context()+" set active done!!!");

            } else {
                WorkManager.getInstance().cancelAllWork();
                NotificationUtil.cancelAllNotification(this);
                try {
                    Thread.sleep(200);
                } catch (Exception e){}
                checkWorker();
            }
        }
    }



    void startLogging(){
        try {
            String cmd = "logcat "+ TAG+":d *:W -f "+logDir.getAbsolutePath()+"/log.txt -r 10000 -n10";
            Log.i(TAG, NameUtil.context()+" Start Logging: "+cmd);
            pLogcat = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            Log.e(TAG, NameUtil.context(),e);
        }
        Log.i(TAG,NameUtil.context()+" Starting Logger finished.");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onCreate() {
        super.onCreate();
        path = getApplicationContext().getExternalFilesDir(null);
        logDir = new File(path, "log");
        logDir.mkdirs();

        startLogging();
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        notification = NotificationUtil.createNotification(this);

        // supervise logging
        new Thread(() -> {
            long TIMEOUT = 60000;
            Log.i(TAG, NameUtil.context()+"logcat supervision: start ");
            int cnt = 0;
            long lastCheck = System.currentTimeMillis();
            while (true){
                try {
                    pLogcat.waitFor(TIMEOUT, TimeUnit.MILLISECONDS );
                    int ec = pLogcat.exitValue(); // normal execution will result in an IllegalStateException
                    Log.e(TAG,NameUtil.context()+"  logcat supervision: logcat process terminated with exitCode "+ec+". Try to start again.");
                    startLogging();
                    lastCheck = System.currentTimeMillis();
                } catch (Exception e) {
                    Log.i(MGChargeApplication.TAG, NameUtil.context()+" logcat supervision "+cnt);

                    long now = System.currentTimeMillis();
                    if (isActive()){
                        if ((now - lastCheck) > (TIMEOUT*1.5)){ // we might have detected an energy saving problem
                            Log.w(TAG, NameUtil.context()+"Log supervision Timeout exceeded by factor 1.5; lastCheck="+lastCheck+" now="+now+" - is there an energy saving problem ?");
                        }
                    }
                    if (++cnt % 5 == 0){
                        Log.i(TAG, NameUtil.context()+"logcat supervision: OK. (running "+(cnt)+" min)");
                    }
                    lastCheck = now;
                }
            }
        }).start();

        Log.i(TAG, "use path: "+path);

        String[] devFiles = path.list((file, s) -> s.startsWith("device_"));
        if (devFiles != null) {
            for (String devFile : devFiles){
                Device device = Device.load(new File(path, devFile));
                if (device != null){
                    devices.add(device);
                }
            }
        }

        String selectedKey = preferences.getString("selectedDevice", "");
        for (Device device : devices) {
            if (selectedKey.equals(device.getId())) {
                setSelectedDevice(device);
                break;
            }
        }
        if ((selectedDevice == null) && (devices.size() > 0)){
            setSelectedDevice(devices.get(0));
        }

        setActive(true);


//        appObservable.setChanged();
        Log.i(TAG, NameUtil.context()+" "+selectedDevice);
    }



    private String deviceFilename(Device device){
        return "device_"+device.getId()+".properties";
    }
    public void saveDevice(Device device){
        device.save(new File(path, deviceFilename(device) ));
    }
    public void removeDevice(Device device){
        boolean res = new File(path, deviceFilename(device) ).delete();
        Log.i(TAG, NameUtil.context()+" removeDevice device="+device+" deviceFilename="+deviceFilename(device)+" res="+res);
    }
    public void setSelectedDevice(Device selectedDevice){
        if (this.selectedDevice != selectedDevice){
            Log.i(TAG, NameUtil.context()+" selectedDevice="+selectedDevice);
            this.selectedDevice = selectedDevice;
            preferences.edit().putString("selectedDevice", selectedDevice.getId() ).apply();
            setDeviceStatus(new DeviceStatus());
        }
    }
    public Device getSelectedDevice(){
        return selectedDevice;
    }



    public void checkWorker(){
        try {
            String uniqueWokName = getApplicationContext().getString(R.string.unique_work_name);
            List<WorkInfo> infos = WorkManager.getInstance().getWorkInfosForUniqueWork(uniqueWokName).get();
            for (WorkInfo info : infos){
                Log.i(TAG, NameUtil.context()+" "+info+ " !!!!");
//                setActive(info.getState() != WorkInfo.State.CANCELLED);
            }
            if (infos.size() > 1){
                Log.e(TAG, NameUtil.context()+ "Unexpected behaviour!");
            }
        } catch (Exception e) {
            Log.e(TAG, NameUtil.context(),e);
        }
    }



    boolean getTargetState(boolean isOn, long now){
        Log.d(TAG, NameUtil.context()+" ckeck shouldBeOn - isOn="+isOn);
        boolean shouldBeOn = isOn;

        if (!isOn){ // if device is switched off
            if (preferences.getBoolean("onCheck", false)){ // and device should switch on ...
                if (preferences.getBoolean("on1", true)){ // ... based on battery level
                    if (getBatteryPercent() < preferences.getInt("onBattery",20)){ // ... and current batteryPercent is less than property onPercent
                        // ... then try to switch on
                        shouldBeOn = true;
                    }
                } else {
                    String[] hhmm = preferences.getString("onTime", "05:00").split(":");
                    if (checkTimeRangeForAction(hhmm, now)){
                        shouldBeOn = true;
                    }
                }
            }

        } else { // ... else device is on ...
            if (preferences.getBoolean("offCheck", false)){ // and device should switch off ...
                if (preferences.getBoolean("off1", true)){ // ... based on battery level
                    if (getBatteryPercent() >= preferences.getInt("offBattery",80)){ // ... and current batteryPercent is less than property onPercent
                        // ... then try to switch off
                        shouldBeOn = false;
                    }
                } else {
                    String[] hhmm = preferences.getString("offTime", "08:00").split(":");
                    if (checkTimeRangeForAction(hhmm, now)){
                        shouldBeOn = false;
                    }
                }
            }
        }
        return shouldBeOn;
    }

    boolean checkTimeRangeForAction(String[] hhmm, long ts){
        long conditionMillis = (Integer.parseInt(hhmm[0])*60L+Integer.parseInt(hhmm[1]))*60*1000L;
        long dayMillis = 24*60*60*1000;
        long timeOfDay = (ts + TimeZone.getDefault().getOffset(ts))% dayMillis;
        long range = 15*60*1000;
        if (conditionMillis + range >= dayMillis){
            timeOfDay += (timeOfDay<range)?dayMillis:0;
        }
        Log.d(TAG, NameUtil.context()+" timeOfDay="+timeOfDay+" conditionMillis="+conditionMillis+" range="+range+" dayMillis="+dayMillis+" ts="+ts);
        return ((conditionMillis <= timeOfDay) && (timeOfDay <= (conditionMillis+range) ));
    }



    private int getBatteryPercent(){
        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        Log.d(TAG, NameUtil.context()
                +" chargeCounter="+bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                +" currentNow="+bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                +" currentAverage="+bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                +" capacity="+bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                +" energyCounter="+bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
                +" status="+bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        );
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }



}
