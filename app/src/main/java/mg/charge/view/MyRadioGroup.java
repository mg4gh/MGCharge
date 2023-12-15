package mg.charge.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RadioGroup;

import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;
import mg.charge.util.IdUtil;

public class MyRadioGroup extends RadioGroup {

    private String idString;
    private IdUtil idUtil;
    private SharedPreferences preferences;

    public MyRadioGroup(Context context) {
        super(context);
        init();
    }

    public MyRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        MGChargeApplication application = ((MGChargeApplication) getContext().getApplicationContext());
        preferences = application.getPreferences();
        idUtil = application.getIdUtil();
        idString = idUtil.getIdString(getId());

        String currentValue = preferences.getString(idString, null);
        Log.i(MGChargeApplication.TAG, NameUtil.context()+" register id="+getId()+" idString="+idString+" currentValue="+currentValue);
        int currentId = idUtil.getId(currentValue);
        check(currentId);

        this.setOnCheckedChangeListener(occl);
    }

    OnCheckedChangeListener occl = (group, checkedId) -> {
        String currentValue = preferences.getString(idString,null);
        String newValue = idUtil.getIdString(checkedId);
        Log.i(MGChargeApplication.TAG, NameUtil.context()+" id="+getId()+" currentValue="+currentValue+" newValue="+newValue);
        preferences.edit().putString(idString, newValue ).apply();
    };

}
