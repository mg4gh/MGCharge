package mg.charge;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    String prefName;
    SharedPreferences preferences;

    public void setPrefName(String prefName, int initialValue) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        this.prefName = prefName;
        setValue(getValue(initialValue));
    }

    public void setValue(int batteryValue){
        preferences.edit().putInt(prefName, batteryValue).apply();
        setText(getResources().getString(R.string.battery_value, batteryValue));
    }

    public int getValue(){
        return preferences.getInt(prefName, -1);
    }
    public int getValue(int defaultValue){
        return preferences.getInt(prefName, defaultValue);
    }


    public void refresh(){
        setText(getResources().getString(R.string.battery_value, getValue()));
    }
}
