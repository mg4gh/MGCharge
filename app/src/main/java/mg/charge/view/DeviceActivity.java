package mg.charge.view;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;
import mg.charge.R;
import mg.charge.model.Device;

public class DeviceActivity extends AppCompatActivity {

    MGChargeApplication mgChargeApplication;
    ArrayList<Device> devices = null;
    Device editDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mgChargeApplication = (MGChargeApplication)getApplication();
        setContentView(R.layout.activity_devices);

        devices = mgChargeApplication.getDevices();
        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle("Devices");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ListView listView = findViewById(R.id.devices_list);
        ArrayAdapter<Device> listAdapter = new ArrayAdapter<>(this, R.layout.text, devices);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            mgChargeApplication.setSelectedDevice( devices.get(i) );
            Log.i(MGChargeApplication.TAG, NameUtil.context()+" idx="+i+" selectedDevice="+ mgChargeApplication.getSelectedDevice());
            startEditDevice(mgChargeApplication.getSelectedDevice());
        });



        TextView newDevice = findViewById(R.id.new_device);
        newDevice.setOnClickListener(view -> startEditDevice( new Device() ));

        TextView deleteDevice = findViewById(R.id.delete_device);
        deleteDevice.setOnClickListener(view -> {
            int idx = devices.indexOf(editDevice);
            if (idx >= 0){
                new AlertDialog.Builder(DeviceActivity.this)
                        .setMessage("Delete device \n      "+editDevice)
                        .setCancelable(false)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            devices.remove(idx);
                            listAdapter.notifyDataSetChanged();
                            mgChargeApplication.removeDevice(editDevice);
                            if (devices.size() == 0){
                                startEditDevice(new Device());
                            } else {
                                mgChargeApplication.setSelectedDevice( devices.get(Math.max(0,idx-1)));
                                startEditDevice(mgChargeApplication.getSelectedDevice());
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });


        TextView editOk = findViewById(R.id.editOk);
        editOk.setOnClickListener(view -> finishEditDevice(listAdapter));

        ((EditText)findViewById(R.id.editPassword)).setImeOptions(EditorInfo.IME_ACTION_DONE);
        ((EditText)findViewById(R.id.editPassword)).setOnEditorActionListener((v, actionId, event) -> {
            Log.d(MGChargeApplication.TAG, NameUtil.context()+" actionId="+actionId);
            if ((actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_UNSPECIFIED)) {
                finishEditDevice(listAdapter);
            }
            return false;
        });

        if (mgChargeApplication.getSelectedDevice() != null){
            startEditDevice(mgChargeApplication.getSelectedDevice());
        }
        mgChargeApplication.activityCountUpdate(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mgChargeApplication.activityCountUpdate(false);
    }

    public void startEditDevice(Device editDevice){
        this.editDevice = editDevice;
        ((TextView)findViewById(R.id.editId)).setText( editDevice.getId() );
        ((EditText)findViewById(R.id.editName)).setText( editDevice.getName() );
        ((EditText)findViewById(R.id.editIpAddress)).setText( editDevice.getIp() );
        ((EditText)findViewById(R.id.editUsername)).setText( editDevice.getUsername() );
        ((EditText)findViewById(R.id.editPassword)).setText( editDevice.getPassword() );
    }

    public void finishEditDevice(ArrayAdapter<Device> listAdapter){
        editDevice.setName(((EditText)findViewById(R.id.editName)).getText().toString() );
        editDevice.setIp(((EditText)findViewById(R.id.editIpAddress)).getText().toString() );
        editDevice.setUsername(((EditText)findViewById(R.id.editUsername)).getText().toString() );
        editDevice.setPassword(((EditText)findViewById(R.id.editPassword)).getText().toString() );
        if (!devices.contains(editDevice)){
            devices.add(editDevice);
        }
        listAdapter.notifyDataSetChanged();
        mgChargeApplication.saveDevice(editDevice);
        mgChargeApplication.setSelectedDevice( editDevice );
        hideKeyboard();
    }


    private void hideKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if((inputMethodManager != null) && (focus != null)){
            inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

}
