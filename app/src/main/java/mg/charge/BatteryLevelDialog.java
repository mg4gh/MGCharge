package mg.charge;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class BatteryLevelDialog extends Dialog implements android.view.View.OnClickListener {

    final private BatteryTextView btw1;
    final private BatteryTextView btw2;
    final private boolean modifyFirst;
    MGRangeSlider slider = null;

    public BatteryLevelDialog(BatteryTextView btw1, BatteryTextView btw2, boolean modifyFirst) {
        super(btw1.getContext());
        this.btw1 = btw1;
        this.btw2 = btw2;
        this.modifyFirst = modifyFirst;
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
        slider.setValues(0f+btw1.getValue(),0f+btw2.getValue());
        slider.setFixIdxAndValue(modifyFirst?1:0,modifyFirst?btw2.getValue(): btw1.getValue());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_yes){
            float value = slider.getValues().get(modifyFirst?0:1);
            BatteryTextView btw = (modifyFirst)?btw1:btw2;
            btw.setValue(Math.round(value));
        }
        dismiss();
    }

    // TODO: make current range values always visible
}
