package mg.charge;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.RangeSlider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class MGRangeSlider extends RangeSlider {

    int fixIdx = -1;
    float fixValue = 0;

    public MGRangeSlider(@NonNull Context context) {
        super(context);
    }

    public MGRangeSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MGRangeSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setFixIdxAndValue(int idx, float value){
        fixIdx = idx;
        fixValue = value;

    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        List<Float> values = super.getValues();
        Log.d(MGChargeApplication.TAG, NameUtil.context()+" "+values);
        if ((0 <= fixIdx) && (fixIdx < values.size())){
            values.remove(fixIdx);
            values.add(fixIdx, fixValue);
        }
        super.setValues(values);
        requestFocus();
        super.onDraw(canvas);

        requestFocus();


    }



    @Override
    public boolean onDragEvent(DragEvent event) {
        Log.d(MGChargeApplication.TAG, NameUtil.context()+"  "+super.getValues());
        return super.onDragEvent(event);
    }

    @NonNull
    @Override
    public List<Float> getValues() {
        Log.d(MGChargeApplication.TAG, NameUtil.context()+"  "+super.getValues());
        return super.getValues();
    }

    @Override
    public void setValues(@NonNull Float... values) {
        Log.d(MGChargeApplication.TAG, NameUtil.context()+"  "+ Arrays.toString(values));
        super.setValues(values);
    }
}
