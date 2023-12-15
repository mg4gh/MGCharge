package mg.charge.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;

import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;

public class MyCheck extends AppCompatCheckBox {

    private String idString;
    private SharedPreferences preferences;

    public MyCheck(@NonNull Context context) {
        super(context);
        init();
    }

    public MyCheck(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyCheck(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        MGChargeApplication application = ((MGChargeApplication) getContext().getApplicationContext());
        preferences = application.getPreferences();
        idString = application.getIdUtil().getIdString(getId());

        boolean currentValue = preferences.getBoolean(idString, true);
        Log.i(MGChargeApplication.TAG, NameUtil.context()+" register id="+getId()+" idString="+idString+" currentValue="+currentValue);
        preferences.edit().putBoolean(idString, currentValue).apply();
        setChecked(currentValue);
    }

    @Override
    public void toggle() {
        super.toggle();
        boolean currentValue = !isChecked();
        Log.i(MGChargeApplication.TAG, NameUtil.context()+" id="+getId()+" idString="+idString+" currentValue="+currentValue+" newValue="+ (!currentValue));
        preferences.edit().putBoolean(idString, !currentValue).apply();
    }
}
