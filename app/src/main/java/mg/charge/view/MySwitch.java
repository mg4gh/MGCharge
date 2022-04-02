package mg.charge.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

public class MySwitch extends SwitchCompat {


    public MySwitch(@NonNull Context context) {
        super(context);
    }

    public MySwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MySwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void toggle() {
        ((MGChargeActivity) getContext()).setDirectSwitch(!isChecked());
    }
}
