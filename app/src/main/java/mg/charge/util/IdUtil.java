package mg.charge.util;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;
import mg.charge.R;

public class IdUtil {

    Context context;
    Map<Integer, String> idMap;

    public IdUtil(Context context){
        this.context = context;
        idMap = new HashMap<>();

        getIdString(R.id.on1);
        getIdString(R.id.on2);
        getIdString(R.id.on);
        getIdString(R.id.off1);
        getIdString(R.id.off2);
        getIdString(R.id.off);
        getIdString(R.id.main);
        getIdString(R.id.onCheck);
        getIdString(R.id.offCheck);
        getIdString(R.id.onBattery);
        getIdString(R.id.onTime);
        getIdString(R.id.offBattery);
        getIdString(R.id.offTime);
        getIdString(R.id.directCheck);
        getIdString(R.id.targetState);
        getIdString(R.id.isActive);

    }

    public String getIdString(int id){
        String idString = idMap.get(id);
        if (idString == null){
            try {
                idString = context.getResources().getResourceEntryName( id);
                idMap.put(id, idString);
            } catch (Exception e){ Log.e(MGChargeApplication.TAG, NameUtil.context()+" "+e.getMessage()); }
        }
        return idString;
    }

    public int getId(String idString){
        if (idString != null){
            for (Map.Entry<Integer,String> entry : idMap.entrySet()){
                if (idString.equals(entry.getValue())){
                    return entry.getKey();
                }
            }
        }
        return -1;
    }
}
