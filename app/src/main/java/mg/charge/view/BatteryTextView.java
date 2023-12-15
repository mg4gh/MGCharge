package mg.charge.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import mg.charge.MGChargeApplication;
import mg.charge.R;

public class BatteryTextView extends androidx.appcompat.widget.AppCompatTextView {

    public BatteryTextView(@NonNull Context context) {
        super(context);
    }

    public BatteryTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryTextView(@NonNull  Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void refresh(int defaultValue){
        MGChargeApplication application = ((MGChargeApplication)getContext().getApplicationContext());
        int value = application.getPreferences().getInt(application.getIdUtil().getIdString(getId()), defaultValue);
        setText(getResources().getString(R.string.battery_value, value));
    }
}
