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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Locale;
import java.util.UUID;

import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;
import mg.charge.R;
import mg.charge.model.DeviceStatus;
import mg.charge.util.IdUtil;

public class MGChargeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    MGChargeApplication mgChargeApplication;
    SharedPreferences preferences;
    IdUtil idUtil;
    Handler timer;

    Runnable ttRefreshDeviceData = new Runnable() {
        @Override
        public void run() {
            preferences.edit().putString(idUtil.getIdString(R.id.main), UUID.randomUUID().toString()).apply(); // used for refresh request
            timer.postDelayed(ttRefreshDeviceData, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(MGChargeApplication.TAG, NameUtil.context());

        mgChargeApplication = (MGChargeApplication) getApplication();
        preferences = mgChargeApplication.getPreferences();
        idUtil = mgChargeApplication.getIdUtil();
        timer = new Handler();

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }
        createTimeOCL(findViewById(R.id.onTime), idUtil.getIdString(R.id.onTime));
        createTimeOCL(findViewById(R.id.offTime), idUtil.getIdString(R.id.offTime));
        findViewById(R.id.onBattery).setOnClickListener(view -> new BatteryLevelDialog(this, R.id.onBattery,R.id.offBattery, true).show());
        findViewById(R.id.offBattery).setOnClickListener(view -> new BatteryLevelDialog(this, R.id.onBattery,R.id.offBattery, false).show());
    }
    private void createTimeOCL(TextView textView, String prefName){
        textView.setOnClickListener(v -> {
            String[] time = textView.getText().toString().split(":");
            TimePickerDialog mTimePicker;
            TimePickerDialog.OnTimeSetListener otsl = (timePicker, selectedHour, selectedMinute) -> {
                String sSelectedTime = String.format(Locale.ENGLISH, "%02d:%02d", selectedHour, selectedMinute);
                preferences.edit().putString(prefName, sSelectedTime).apply();
                textView.setText(sSelectedTime);
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
            Log.i(MGChargeApplication.TAG, NameUtil.context()+" menu item exit pressed ... leaving application");
            exitApplication(0);
        }
        return res;
    }

    void exitApplication(int activityCount){
        Log.i(MGChargeApplication.TAG, NameUtil.context());
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
        preferences.registerOnSharedPreferenceChangeListener(this);
        mgChargeApplication.activityCountUpdate(true);
        setHead();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(MGChargeApplication.TAG, NameUtil.context());
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        timer.removeCallbacks(ttRefreshDeviceData);
        mgChargeApplication.activityCountUpdate(false);
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
                text = getResources().getString(R.string.connected) + String.format(Locale.ENGLISH," - %s - %s - %.1fW",
                        mgChargeApplication.isActive()?"active":"inactive",
                        deviceStatus.isOn()?"on":"off",
                        deviceStatus.getPower());
            } else {
                text = getResources().getString(R.string.unconnected);
            }
            ((TextView)findViewById(R.id.state)).setText(text);
        } else {
            ((TextView)findViewById(R.id.device)).setText(getResources().getString(R.string.app_name));
        }

    }
    private void setOnGroup(){
        setEnabledDependencies(findViewById(R.id.onCheck), findViewById(R.id.on1), findViewById(R.id.onBattery), findViewById(R.id.on2), findViewById(R.id.onTime));
        ((MyRadioGroup)findViewById(R.id.on)).check(idUtil.getId( preferences.getString(idUtil.getIdString(R.id.on), idUtil.getIdString(R.id.on2)) ));
        ((BatteryTextView)findViewById(R.id.onBattery)).refresh(20);
        ((TextView)findViewById(R.id.onTime)).setText(preferences.getString(idUtil.getIdString(R.id.onTime), "06:00"));
    }

    private void setOffGroup(){
        setEnabledDependencies(findViewById(R.id.offCheck), findViewById(R.id.off1), findViewById(R.id.offBattery), findViewById(R.id.off2), findViewById(R.id.offTime));
        ((MyRadioGroup)findViewById(R.id.off)).check(idUtil.getId( preferences.getString(idUtil.getIdString(R.id.off), idUtil.getIdString(R.id.off1)) ));
        ((BatteryTextView)findViewById(R.id.offBattery)).refresh(85);
        ((TextView)findViewById(R.id.offTime)).setText(preferences.getString(idUtil.getIdString(R.id.offTime), "08:00"));
    }


    private void setDirectGroup(){
        DeviceStatus deviceStatus = mgChargeApplication.getDeviceStatus();

        setEnabledDependencies(((CheckBox)findViewById(R.id.directCheck)).isChecked() && deviceStatus.isConnected(), findViewById(R.id.targetState), findViewById(R.id.targetStateDescription),
                findViewById(R.id.isActive), findViewById(R.id.isActiveDescription));
        Log.i(MGChargeApplication.TAG, NameUtil.context()+ " mgChargeApplication.isOn="+deviceStatus.isOn());

        ((SwitchCompat)findViewById(R.id.targetState)).setChecked(preferences.getBoolean(idUtil.getIdString(R.id.targetState), false));
        ((SwitchCompat)findViewById(R.id.isActive)).setChecked(preferences.getBoolean(idUtil.getIdString(R.id.isActive), true));

    }



    private void setEnabledDependencies(CheckBox checkBox, View ... views){
        setEnabledDependencies(checkBox.isChecked(), views);
    }
    private void setEnabledDependencies(boolean enabled, View ... views){
        for (View view : views){
            view.setEnabled(enabled);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (idUtil.getIdString(R.id.isActive).equals(key)
                || idUtil.getIdString(R.id.directCheck).equals(key)
                || idUtil.getIdString(R.id.on).equals(key)
                || idUtil.getIdString(R.id.onCheck).equals(key)
                || idUtil.getIdString(R.id.off).equals(key)
                || idUtil.getIdString(R.id.offCheck).equals(key)
                || idUtil.getIdString(R.id.targetState).equals(key)
                || idUtil.getIdString(R.id.state).equals(key)
                || idUtil.getIdString(R.id.onTime).equals(key)
                || idUtil.getIdString(R.id.offTime).equals(key)
                || idUtil.getIdString(R.id.onBattery).equals(key)
                || idUtil.getIdString(R.id.offBattery).equals(key)
        ){
            runOnUiThread(() -> {
                Log.i(MGChargeApplication.TAG, NameUtil.context()+" update UI of MGChargeActivity!");
                setHead();
                setOnGroup();
                setOffGroup();
                setDirectGroup();
            });
        }
    }

}