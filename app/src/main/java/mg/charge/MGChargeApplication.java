package mg.charge;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MGChargeApplication extends Application {

    public static final String TAG = "MGCharge";

    SharedPreferences preferences;

    ArrayList<Device> devices = new ArrayList<>();
    Device selectedDevice = null;
    MGObservable appObservable = new MGObservable();

    File path;
    File logDir = null;
    private Process pLogcat = null;

    /** indicates whether application is active (not active after auto switch off, before app is opened again) */
    boolean active = true;
    /** indicates whether application is connected to the selected device */
    boolean connected = false;
    /** indicates whether selected device is switched on (only valid, if <b>connected</b> is set) */
    volatile boolean isOn = false;
    /** indicates that selected device should be switched on (only valid, if <b>connected</b> is set) */
    volatile boolean shouldBOn = false;

    /** indicates whether SupervisionService is currently running */
    volatile boolean superviseDevice = false;
    /** indicates whether SupervisionService should be running */
    volatile boolean shouldSuperviseDevice = false;
    /** indicates whether MGChargeActivity is currently visible (after onResume and before onPause) */
    boolean mgChargeActivityRunning = false;
    /** last power consumption measured */
    float powerMeter = 0;




    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active){
            Log.i(TAG, NameUtil.context()+" set active="+active);
            this.active = active;
        }
    }

    public boolean isConnected() {
        return connected;
    }
    public void setConnected(boolean connected) {
        if (this.connected != connected){
            this.connected = connected;
            Log.i(TAG, NameUtil.context()+" set connected="+connected);
        }
    }

    public void setOn(boolean on) {
        if (isOn != on){
            isOn = on;
            Log.i(TAG, NameUtil.context()+" set isOn="+on);
        }
    }

    public void setShouldBOn(boolean shouldBOn) {
        if (this.shouldBOn != shouldBOn){
            this.shouldBOn = shouldBOn;
            Log.i(TAG, NameUtil.context()+" set shouldOn="+ shouldBOn);
        }
    }

    public boolean isShouldSuperviseDevice() {
        return shouldSuperviseDevice;
    }

    public void setShouldSuperviseDevice(boolean shouldSuperviseDevice) {
        if (this.shouldSuperviseDevice != shouldSuperviseDevice){
            this.shouldSuperviseDevice = shouldSuperviseDevice;
            Log.i(TAG, NameUtil.context()+" set shouldSuperviseDevice="+shouldSuperviseDevice);
            triggerSupervisionService();
        }
    }

    public void setSuperviseDevice(boolean superviseDevice) {
        if (this.superviseDevice != superviseDevice){
            this.superviseDevice = superviseDevice;
            Log.i(TAG, NameUtil.context()+" set superviseDevice="+shouldSuperviseDevice);
        }
    }

    public void setPowerMeter(float powerMeter) {
        this.powerMeter = powerMeter;
    }

    public void wakeupDeviceHandler(){
        appObservable.setChanged();
        Log.d(TAG, NameUtil.context());
        synchronized (this){
            notifyAll();
        }
    }

    public void triggerSupervisionService(){
        Intent intent = new Intent(this, SupervisionService.class);
        this.startForegroundService(intent);
        Log.d(TAG, NameUtil.context());
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
                    if (superviseDevice){
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

        createDeviceHandler();
        appObservable.setChanged();
        Log.i(TAG, "onCreate: "+selectedDevice);
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
            connected = false;
            appObservable.setChanged();
            appObservable.notifyObservers();
        }
    }



    void createDeviceHandler(){
        new Thread(() -> {
            while (true){
                long start = System.currentTimeMillis();
                Log.i(TAG, NameUtil.context()+" deviceHandler Supervision");
                try {
                    if (isOn != shouldBOn){
                        deviceAction(selectedDevice, shouldBOn);
                    } else {

                        // now check for auto action
                        if (isActive()){ // do nothing while inactive
                            Log.d(TAG, NameUtil.context()+" checkAutoActions");
                            if (connected){ // ... and device is connected
                                Log.d(TAG, NameUtil.context()+" device is connected!");
                                if (!isOn){ // ... and device is switched off
                                    if (preferences.getBoolean("onCheck", false)){ // and device should switch on ...
                                        if (preferences.getBoolean("on1", true)){ // ... based on battery level
                                            if (getBatteryPercent() < preferences.getInt("onBattery",20)){ // ... and current batteryPercent is less than property onPercent
                                                // ... then try to switch on
                                                setShouldBOn(true);
                                            }
                                        } else {
                                            String[] hhmm = preferences.getString("onTime", "05:00").split(":");
                                            if (checkTimeRangeForAction(hhmm, start)){
                                                setShouldBOn(true);
                                            }
                                        }
                                    }

                                } else { // ... else device is on ...
                                    if (preferences.getBoolean("offCheck", false)){ // and device should switch off ...
                                        if (preferences.getBoolean("off1", true)){ // ... based on battery level
                                            if (getBatteryPercent() >= preferences.getInt("offBattery",80)){ // ... and current batteryPercent is less than property onPercent
                                                // ... then try to switch on
                                                setShouldBOn(false);
                                            }
                                        } else {
                                            String[] hhmm = preferences.getString("offTime", "08:00").split(":");
                                            if (checkTimeRangeForAction(hhmm, start)){
                                                setShouldBOn(false);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, NameUtil.context()+" active: "+isActive());
                        }


                        if (isOn != shouldBOn){ // auto action required
                            deviceAction(selectedDevice, shouldBOn);

                            if (!shouldBOn && !isOn && !mgChargeActivityRunning){
                                Log.i(TAG, NameUtil.context()+" trigger MGChargeActivity to exit");
                                Intent intent = new Intent(MGChargeApplication.this, MGChargeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("EXIT", true);
                                startActivity(intent);
                            }
                        } else {
                            if (isActive()){
                                deviceAction(selectedDevice, null);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, NameUtil.context(), e);
                }

                long now = System.currentTimeMillis();
                long to = (mgChargeActivityRunning?5000:65000) - (now-start);
                try {
                    synchronized (MGChargeApplication.this){
                        if (isOn == shouldBOn){
                            MGChargeApplication.this.wait(to);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, NameUtil.context(), e);
                }
            }
        }).start();
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

    public void deviceAction(Device dev, Boolean onOff){
        if (dev == null) return;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build();

            String credential = Credentials.basic(dev.getUsername(), dev.getPassword());
            String urlPart = (onOff==null)?"/status":("/relay/0?turn="+(onOff?"on":"off"));
            Request.Builder requestBuilder = new Request.Builder()
                    .header("Authorization", credential)
                    .url("http://"+dev.getIp()+urlPart);
            Request request = requestBuilder.build();

            Call call = client.newCall(request);
            Response response = call.execute();
            Log.d(TAG, NameUtil.context()+" rc="+response.code());


            if (response.code() == 200){
                ResponseBody responseBody = response.body();
                if (responseBody != null){
                    String answer = responseBody.string();
                    Log.d(TAG, NameUtil.context()+" response="+response);
                    Log.d(TAG, NameUtil.context()+" body="+answer);

                    setConnected(true);
                    String[] part = answer.split("\"ison\":");
                    setOn ((part.length == 2) && (part[1].startsWith("true")));
                    setShouldBOn(isOn);

                    try {
                        String power = answer.replaceAll(".*\"power\":","").replaceFirst(",.*","");
                        setPowerMeter( Float.parseFloat(power) );
                    } catch (NumberFormatException e) {
                        Log.d(TAG, NameUtil.context()+" no power value detected");
                    }
                }

            } else {
                setConnected(false);
            }
        } catch (Throwable e) {
            setConnected(false);
            if (e instanceof SocketTimeoutException){
                Log.w(TAG, NameUtil.context()+ " SocketTimeoutException occurred");
            } else {
                Log.e(TAG, NameUtil.context(), e);
            }
        }
        Log.i(TAG, NameUtil.context()+" state active="+isActive()+" state connected="+connected+" isOn="+isOn+" shouldOn="+ shouldBOn
                  +" superviseDevice="+superviseDevice+" shouldSuperviseDevice="+shouldSuperviseDevice+" powerMeter="+powerMeter+" device=\""+selectedDevice+"\"");
        appObservable.setChanged();
        appObservable.notifyObservers();


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
