package mg.charge;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import mg.charge.control.DeviceActionGetStatus;
import mg.charge.control.DeviceActionTurn;
import mg.charge.model.Device;
import mg.charge.model.DeviceStatus;
import mg.charge.util.IdUtil;
import mg.charge.util.NotificationUtil;

public class MGChargeApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "MGCharge";

    SharedPreferences preferences;

    ArrayList<Device> devices = new ArrayList<>();
    Device selectedDevice = null;
    int activityCount = 0;
    DeviceStatus deviceStatus = new DeviceStatus();
    boolean targetStateChanging = false;
    IdUtil idUtil;

    File path;
    File logDir = null;
    private Process pLogcat = null;
    Notification notification = null;

    public SharedPreferences getPreferences() {
        return preferences;
    }


    public ArrayList<Device> getDevices() {
        return devices;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public IdUtil getIdUtil() {
        return idUtil;
    }

    public void activityCountUpdate(boolean resume){
        activityCount += resume?1:-1;
        Log.i(TAG, NameUtil.context()+" activityCount="+activityCount);
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
        preferences.edit().putString(idUtil.getIdString(R.id.state), UUID.randomUUID().toString()).apply(); // this is only the trigger for listeners, not the value
        if (preferences.getBoolean(idUtil.getIdString(R.id.targetState), false) == deviceStatus.isOn() ){
            targetStateChanging = false;
        } else {
            if (!targetStateChanging){
                preferences.edit().putBoolean(idUtil.getIdString(R.id.targetState), deviceStatus.isOn()).apply();
            }
        }
    }

    public boolean isActive() {
        return preferences.getBoolean(idUtil.getIdString(R.id.isActive),false);
    }

    public void setActive(boolean active) {
        if (isActive() != active){
            Log.i(TAG, NameUtil.context()+" set active="+active);
            preferences.edit().putBoolean(idUtil.getIdString(R.id.isActive), active ).apply();
        } else {
            Log.i(TAG, NameUtil.context()+" set active="+active+" - is already set - no action");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, NameUtil.context()+" key="+key+" value="+sharedPreferences.getAll().get(key));
        if (idUtil.getIdString(R.id.isActive).equals(key)){
            onSetActive(isActive());
        } else if (idUtil.getIdString(R.id.main).equals(key)){
            onRefresh();
        } else if (idUtil.getIdString(R.id.targetState).equals(key)){
            onTargetState();
        }
    }

    private void onSetActive(boolean active){
        if (active){
            Log.i(TAG, NameUtil.context()+" on set active=true");
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DeviceWorker.class)
                    .setInitialDelay(1, TimeUnit.SECONDS).build();
            String uniqueWokName = getApplicationContext().getString(R.string.unique_work_name);
            WorkManager.getInstance(this).enqueueUniqueWork(uniqueWokName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);
            NotificationUtil.showNotification(this, notification);
            Log.i(TAG, NameUtil.context()+" on set active=true done");
        } else {
            Log.i(TAG, NameUtil.context()+" on set active=false");
            WorkManager.getInstance(this).cancelAllWork();
            NotificationUtil.cancelAllNotification(this);
            try {
                Thread.sleep(200);
            } catch (Exception e){ Log.e(TAG, NameUtil.context()+" Interrupt occurred" ); }
            checkWorker();
            Log.i(TAG, NameUtil.context()+" on set active=false done");
        }
    }

    private void onRefresh(){
        new Thread(() -> {
            DeviceStatus deviceStatus = new DeviceActionGetStatus(selectedDevice).execute();
            setDeviceStatus(deviceStatus);
            checkWorker();
        }).start();
    }

    private void onTargetState(){
        boolean targetState = preferences.getBoolean(idUtil.getIdString(R.id.targetState), false);
        if (targetState != deviceStatus.isOn()){
            targetStateChanging = true;
            new Thread(() -> {
                new DeviceActionTurn(getSelectedDevice(), targetState).execute();
                DeviceStatus deviceStatus = new DeviceActionGetStatus(selectedDevice).execute();
                setDeviceStatus(deviceStatus);
            }).start();
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
        String preferencesName = this.getPackageName() + "_preferences";
        preferences = this.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        idUtil = new IdUtil(this);

        preferences.registerOnSharedPreferenceChangeListener(this);
        notification = NotificationUtil.createNotification(this);

        // supervise logging
        new Thread(() -> {
            long TIMEOUT = 10000;
            Log.i(TAG, NameUtil.context()+"logcat supervision: start ");
            int cnt = 0;
            long lastCheck = System.currentTimeMillis();
            while (true){
                try {
                    cnt++;
                    pLogcat.waitFor(TIMEOUT, TimeUnit.MILLISECONDS );
                    int ec = pLogcat.exitValue(); // normal execution will result in an IllegalStateException
                    Log.e(TAG,NameUtil.context()+"  logcat supervision: logcat process terminated with exitCode "+ec+". Try to start again.");
                    startLogging();
                    lastCheck = System.currentTimeMillis();
                } catch (Exception e) {
                    Log.v(MGChargeApplication.TAG, NameUtil.context()+" logcat supervision "+cnt);

                    long now = System.currentTimeMillis();
                    if (isActive()){
                        if ((now - lastCheck) > (TIMEOUT*1.5)){ // we might have detected an energy saving problem
                            Log.w(TAG, NameUtil.context()+"Log supervision Timeout exceeded by factor 1.5; lastCheck="+lastCheck+" now="+now);
                        }
                    }
                    if (cnt % 6 == 0){
                        Log.i(TAG, NameUtil.context()+"logcat supervision: OK. (running "+(cnt / 6)+" min)");
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

        setActive(false);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { Log.e(TAG, NameUtil.context()+ "Unexpected interruption!"); }
        setActive(true);


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
            List<WorkInfo> infos = WorkManager.getInstance(this).getWorkInfosForUniqueWork(uniqueWokName).get();
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



    boolean getTargetState(boolean isOn, long now, long[] retriggerTime){
        Log.d(TAG, NameUtil.context()+" ckeck shouldBeOn - isOn="+isOn);
        boolean shouldBeOn = isOn;

        if (!isOn){ // if device is switched off
            if (preferences.getBoolean(idUtil.getIdString(R.id.onCheck), false)){ // and device should switch on ...
                if (preferences.getString(idUtil.getIdString(R.id.on), idUtil.getIdString(R.id.on2)).equals( idUtil.getIdString(R.id.on1) )){ // ... based on battery level
                    shouldBeOn = checkBatteryForActionOn(getBatteryPercent(), preferences.getInt(idUtil.getIdString(R.id.onBattery),20), retriggerTime); // shouldBeOn = current batteryPercent is less than property onPercent
                } else {
                    String[] hhmm = preferences.getString( idUtil.getIdString(R.id.onTime), "06:00").split(":");
                    shouldBeOn =  (checkTimeRangeForAction(hhmm, now, retriggerTime));
                }
            }

        } else { // ... else device is on ...
            if (preferences.getBoolean(idUtil.getIdString(R.id.offCheck), false)){ // and device should switch off ...
                if (preferences.getString(idUtil.getIdString(R.id.off), idUtil.getIdString(R.id.off1)).equals( idUtil.getIdString(R.id.off1))){ // ... based on battery level
                    shouldBeOn = checkBatteryForActionOff(getBatteryPercent(), preferences.getInt(idUtil.getIdString(R.id.offBattery),80), retriggerTime); // shouldBeOn = current batteryPercent is less than property onPercent
                } else {
                    String[] hhmm = preferences.getString(idUtil.getIdString(R.id.offTime), "08:00").split(":");
                    shouldBeOn = !checkTimeRangeForAction(hhmm, now, retriggerTime);
                }
            }
        }
        Log.d(TAG, NameUtil.context()+" ckeck shouldBeOn="+shouldBeOn+" - isOn="+isOn);
        return shouldBeOn;
    }

    boolean checkTimeRangeForAction(String[] hhmm, long ts, long[] retriggerTime){
        long conditionMillis = (Integer.parseInt(hhmm[0])*60L+Integer.parseInt(hhmm[1]))*60*1000L;
        long dayMillis = 24*60*60*1000;
        long timeOfDay = (ts + TimeZone.getDefault().getOffset(ts))% dayMillis;
        long range = 30*60*1000;
        if (conditionMillis + range >= dayMillis){
            timeOfDay += (timeOfDay<range)?dayMillis:0;
        }
        retriggerTime[0] = (conditionMillis - timeOfDay)/(1000*60);
        Log.d(TAG, NameUtil.context()+" timeOfDay="+timeOfDay+" conditionMillis="+conditionMillis+" range="+range+" dayMillis="+dayMillis+" ts="+ts+" retriggerTime="+retriggerTime[0]);
        return ((conditionMillis <= timeOfDay) && (timeOfDay <= (conditionMillis+range) ));
    }

    boolean checkBatteryForActionOff(int currentBat, int batThreshold, long[] retriggerTime) {
        return !checkBatteryForActionOn(100-currentBat, 100-batThreshold, retriggerTime);
    }
    boolean checkBatteryForActionOn(int currentBat, int batThreshold, long[] retriggerTime){
        retriggerTime[0] = (long)Math.pow(2, Math.min(currentBat, batThreshold+10)-batThreshold);
        Log.d(TAG, NameUtil.context()+" currentBat="+currentBat+" batThreshold="+batThreshold+" retriggerTime="+retriggerTime[0]);
        return currentBat < batThreshold;
    }



    private int getBatteryPercent(){
        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        Log.v(TAG, NameUtil.context()
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
