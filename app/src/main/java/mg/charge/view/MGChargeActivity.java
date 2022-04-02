package mg.charge.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;
import mg.charge.R;
import mg.charge.control.DeviceActionGetStatus;
import mg.charge.control.DeviceActionTurn;
import mg.charge.model.DeviceStatus;

public class MGChargeActivity extends AppCompatActivity implements Observer {

    MGChargeApplication mgChargeApplication;
    SharedPreferences preferences;
    Handler timer;

    Runnable ttRefreshDeviceData = new Runnable() {
        @Override
        public void run() {
            new Thread(){
                @Override
                public void run() {
                    DeviceStatus deviceStatus = new DeviceActionGetStatus(mgChargeApplication.getSelectedDevice()).execute();
                    mgChargeApplication.setDeviceStatus(deviceStatus);
                    mgChargeApplication.checkWorker();
                }
            }.start();
            timer.postDelayed(ttRefreshDeviceData, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(MGChargeApplication.TAG, NameUtil.context());

        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        mgChargeApplication = (MGChargeApplication) getApplication();
        timer = new Handler();

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }

        CheckBox direct = findViewById(R.id.directCheck);
        direct.setOnClickListener(view -> setEnabledDependencies(direct, findViewById(R.id.directSwitch), findViewById(R.id.directDescription)));

        ((SwitchCompat)findViewById(R.id.directSwitch)).setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (compoundButton.isPressed()){
                Log.i("TAG", NameUtil.context()+" set isChecked="+isChecked);
            }
        });


        ((CheckBox)findViewById(R.id.onCheck)).setOnCheckedChangeListener((compoundButton, isChecked) -> {
            preferences.edit().putBoolean("onCheck", isChecked).apply();
            mgChargeApplication.setActive( isChecked || ((CheckBox)findViewById(R.id.offCheck)).isChecked() );
            setOnGroup();
        });
        ((CheckBox)findViewById(R.id.offCheck)).setOnCheckedChangeListener((compoundButton, isChecked) -> {
            preferences.edit().putBoolean("offCheck", isChecked).apply();
            mgChargeApplication.setActive( isChecked || ((CheckBox)findViewById(R.id.onCheck)).isChecked() );
            setOffGroup();
        });
        ((CheckBox)findViewById(R.id.directCheck)).setOnCheckedChangeListener((compoundButton, isChecked) -> {
            Log.i(MGChargeApplication.TAG, "CB: directCheck="+isChecked);
            preferences.edit().putBoolean("directCheck", isChecked).apply();
            setDirectGroup();
        });

        ((RadioButton)findViewById(R.id.on1)).setOnCheckedChangeListener((compoundButton, isChecked) -> preferences.edit().putBoolean("on1", isChecked).commit() );
        ((RadioButton)findViewById(R.id.on2)).setOnCheckedChangeListener((compoundButton, isChecked) -> preferences.edit().putBoolean("on2", isChecked).commit() );
        ((RadioButton)findViewById(R.id.off1)).setOnCheckedChangeListener((compoundButton, isChecked) -> preferences.edit().putBoolean("off1", isChecked).commit() );
        ((RadioButton)findViewById(R.id.off2)).setOnCheckedChangeListener((compoundButton, isChecked) -> preferences.edit().putBoolean("off2", isChecked).commit() );

        createTimeOCL(findViewById(R.id.onTime), "onTime");
        createTimeOCL(findViewById(R.id.offTime), "offTime");

        BatteryTextView btwOn = findViewById(R.id.onBattery);
        BatteryTextView btwOff = findViewById(R.id.offBattery);
        btwOn.setPrefName("onBattery", 20);
        btwOff.setPrefName("offBattery", 85);
        btwOn.setOnClickListener(view -> new BatteryLevelDialog(btwOn, btwOff, true).show());
        btwOff.setOnClickListener(view -> new BatteryLevelDialog(btwOn, btwOff, false).show());
    }
    private void createTimeOCL(TextView textView, String prefName){
        textView.setOnClickListener(v -> {
            String[] time = textView.getText().toString().split(":");
            TimePickerDialog mTimePicker;
            TimePickerDialog.OnTimeSetListener otsl = new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    String sSelectedTime = String.format(Locale.ENGLISH, "%02d:%02d", selectedHour, selectedMinute);
                    preferences.edit().putString(prefName, sSelectedTime).apply();
                    textView.setText(sSelectedTime);
                }
            };
            mTimePicker = new TimePickerDialog(MGChargeActivity.this, otsl, Integer.parseInt(time[0]), Integer.parseInt(time[1]),true);
            mTimePicker.setTitle("Select Time");
            mTimePicker.show();
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("EXIT", false)) {
            Log.i(MGChargeApplication.TAG, NameUtil.context()+" "+intent.getIntExtra("ACTIVITY_COUNT",0));
            exitApplication(intent.getIntExtra("ACTIVITY_COUNT",0));
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(MGChargeApplication.TAG, NameUtil.context());
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean res = super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.menu_devices){
            startDevicesActivity();
        }
        if (item.getItemId() == R.id.menu_about){
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getResources().getString(R.string.app_name));
            alertDialog.setMessage(getString(R.string.app_version));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                    (dialog, which) -> dialog.dismiss());
            alertDialog.show();
        }
        if (item.getItemId() == R.id.menu_exit){
            exitApplication(0);
        }
        return res;
    }

    void exitApplication(int activityCount){
        Log.i(MGChargeApplication.TAG, NameUtil.context());
        mgChargeApplication.setActive(false);
        if (activityCount != 1){
            finishAndRemoveTask();
            Log.i(MGChargeApplication.TAG, NameUtil.context());
            System.exit(0);
        }
    }

    void startDevicesActivity(){
        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(MGChargeApplication.TAG, NameUtil.context());
        timer.postDelayed(ttRefreshDeviceData, 10);
//        mgChargeApplication.setActive(true);
        mgChargeApplication.getAppObservable().addObserver(this);
        mgChargeApplication.actvityCountUpdate(true);
        setHead();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(MGChargeApplication.TAG, NameUtil.context());
        mgChargeApplication.getAppObservable().deleteObserver(this);
        timer.removeCallbacks(ttRefreshDeviceData);
//        mgChargeApplication.setMgChargeActivityRunning(false);
        mgChargeApplication.actvityCountUpdate(false);
    }

    @Override
    protected void onStop() {
        Log.i(MGChargeApplication.TAG, NameUtil.context());
        super.onStop();
    }

    private void setHead(){
        if (mgChargeApplication.getSelectedDevice() != null){
            ((TextView)findViewById(R.id.device)).setText(mgChargeApplication.getSelectedDevice().getName());
            String text;
            DeviceStatus deviceStatus = mgChargeApplication.getDeviceStatus();
            if (deviceStatus.isConnected() ){
                text = getResources().getString(R.string.connected) + String.format(Locale.ENGLISH," - %s - %.1fW",deviceStatus.isOn()?"on":"off",deviceStatus.getPower());
            } else {
                text = getResources().getString(R.string.unconnected);
            }
            ((TextView)findViewById(R.id.connected)).setText(text);
        } else {
            ((TextView)findViewById(R.id.device)).setText(getResources().getString(R.string.app_name));
        }

    }
    private void setOnGroup(){
        ((CheckBox)findViewById(R.id.onCheck)).setChecked(preferences.getBoolean("onCheck", false));
        setEnabledDependencies(findViewById(R.id.onCheck), findViewById(R.id.on1), findViewById(R.id.onBattery), findViewById(R.id.on2), findViewById(R.id.onTime));
        ((RadioButton)findViewById(R.id.on1)).setChecked(preferences.getBoolean("on1", true));
        ((BatteryTextView)findViewById(R.id.onBattery)).refresh();
        ((RadioButton)findViewById(R.id.on2)).setChecked(preferences.getBoolean("on2", false));
        ((TextView)findViewById(R.id.onTime)).setText(preferences.getString("onTime", "05:00"));
    }

    private void setOffGroup(){
        setEnabledDependencies(findViewById(R.id.offCheck), findViewById(R.id.off1), findViewById(R.id.offBattery), findViewById(R.id.off2), findViewById(R.id.offTime));
        ((CheckBox)findViewById(R.id.offCheck)).setChecked(preferences.getBoolean("offCheck", false));
        ((RadioButton)findViewById(R.id.off1)).setChecked(preferences.getBoolean("off1", true));
        ((BatteryTextView)findViewById(R.id.offBattery)).refresh();
        ((RadioButton)findViewById(R.id.off2)).setChecked(preferences.getBoolean("off2", false));
        ((TextView)findViewById(R.id.offTime)).setText(preferences.getString("offTime", "08:00"));
    }

    private void setDirectHeadGroup(){
        DeviceStatus deviceStatus = mgChargeApplication.getDeviceStatus();
        ((CheckBox)findViewById(R.id.directCheck)).setChecked(preferences.getBoolean("directCheck", false));
        setEnabledDependencies(deviceStatus.isConnected(), findViewById(R.id.directHead), findViewById(R.id.directCheck), findViewById(R.id.directDescription), findViewById(R.id.directSwitch) );
        findViewById(R.id.directCheck).setEnabled(deviceStatus.isConnected());
        findViewById(R.id.directHead).setEnabled(deviceStatus.isConnected());
        if (deviceStatus.isConnected()){
            ((SwitchCompat)findViewById(R.id.directSwitch)).setChecked(deviceStatus.isOn());
        }
    }

    private void setDirectGroup(){
        DeviceStatus deviceStatus = mgChargeApplication.getDeviceStatus();

        setEnabledDependencies(findViewById(R.id.directCheck), findViewById(R.id.directSwitch), findViewById(R.id.directDescription));
        Log.i(MGChargeApplication.TAG, NameUtil.context()+ " mgChargeApplication.isOn="+deviceStatus.isOn());
        if (deviceStatus.isConnected()){
            ((SwitchCompat)findViewById(R.id.directSwitch)).setChecked(deviceStatus.isOn());
        }
    }



    private void setEnabledDependencies(CheckBox checkBox, View ... views){
        setEnabledDependencies(checkBox.isChecked(), views);
    }
    private void setEnabledDependencies(boolean enabled, View ... views){
        for (View view : views){
            view.setEnabled(enabled);
        }
    }

    public void setDirectSwitch(boolean setChecked){
        Log.i(MGChargeApplication.TAG, NameUtil.context()+" setChecked="+ setChecked);
        if (mgChargeApplication.getDeviceStatus().isOn() != setChecked){
            new Thread(){
                @Override
                public void run() {
                    new DeviceActionTurn(mgChargeApplication.getSelectedDevice(), setChecked).execute();
                    DeviceStatus deviceStatus = new DeviceActionGetStatus(mgChargeApplication.getSelectedDevice()).execute();
                    mgChargeApplication.setDeviceStatus(deviceStatus);
                }
            }.start();
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        runOnUiThread(() -> {
            Log.i(MGChargeApplication.TAG, NameUtil.context()+" update MGChargeActivity!");
            setHead();
            setDirectHeadGroup();
            setDirectGroup();
            setOnGroup();
            setOffGroup();
        });
    }
}