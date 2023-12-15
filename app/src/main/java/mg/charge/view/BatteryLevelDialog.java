package mg.charge.view;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import mg.charge.MGChargeApplication;
import mg.charge.R;

public class BatteryLevelDialog extends Dialog implements android.view.View.OnClickListener {

    final private int btwId1;
    final private int btwId2;
    final private boolean modifyFirst;
    MGRangeSlider slider = null;
    MGChargeApplication application;
    SharedPreferences preferences;

    public BatteryLevelDialog(Context context, int btwId1, int btwId2, boolean modifyFirst) {
        super(context);
        this.btwId1 = btwId1;
        this.btwId2 = btwId2;
        this.modifyFirst = modifyFirst;
        application = ((MGChargeApplication)context.getApplicationContext());
        preferences = application.getPreferences();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.level_dialog);
        findViewById(R.id.btn_yes).setOnClickListener(this);
        findViewById(R.id.btn_no).setOnClickListener(this);

        ((TextView)findViewById(R.id.txt_dia)).setText(getContext().getResources().getString(R.string.battery_level_dialog_title, modifyFirst?"on":"off"));
        slider = findViewById(R.id.slide);
        slider.setValueFrom(0);
        slider.setValueTo(100);
        float value1 = preferences.getInt(application.getIdUtil().getIdString(btwId1), 20);
        float value2 = preferences.getInt(application.getIdUtil().getIdString(btwId2), 85);
        slider.setValues(value1,value2);
        slider.setFixIdxAndValue(modifyFirst?1:0,modifyFirst?value2: value1);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_yes){
            float value = slider.getValues().get(modifyFirst?0:1);
            preferences.edit().putInt(application.getIdUtil().getIdString((modifyFirst)?btwId1:btwId2), Math.round(value)).apply();
        }
        dismiss();
    }

    // TODO: make current range values always visible
}
