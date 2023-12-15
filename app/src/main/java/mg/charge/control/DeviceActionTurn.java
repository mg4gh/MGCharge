package mg.charge.control;

import android.util.Log;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import mg.charge.model.Device;
import mg.charge.MGChargeApplication;
import mg.charge.NameUtil;
import mg.charge.model.DeviceStatus;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DeviceActionTurn extends DeviceAction{

    boolean onOff;

    public DeviceActionTurn(Device device, boolean onOff){
        super(device);
        this.onOff =onOff;
    }

    @Override
    public DeviceStatus execute() {
        if (device == null){
            Log.w(MGChargeApplication.TAG, NameUtil.context()+ " cannot turn - dev=null");
            return null;
        }
        try {
            Log.d(MGChargeApplication.TAG, NameUtil.context()+" onOff="+onOff);
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build();

            String credential = Credentials.basic(device.getUsername(), device.getPassword());
            String urlPart = "/relay/0?turn="+(onOff?"on":"off");
            String url = "http://"+device.getIp()+urlPart;
            Request.Builder requestBuilder = new Request.Builder()
                    .header("Authorization", credential)
                    .url(url);
            Request request = requestBuilder.build();

            Call call = client.newCall(request);
            try (Response response = call.execute()){
                Log.d(MGChargeApplication.TAG, NameUtil.context()+ " url="+url+" rc="+response.code());
                if (response.code() == 200){
                    ResponseBody responseBody = response.body();
                    if (responseBody != null){
                        Log.d(MGChargeApplication.TAG, NameUtil.context()+" response="+response);
                        try {
                            String answer = responseBody.string();
                            Log.d(MGChargeApplication.TAG, NameUtil.context()+" body="+answer);
                        } catch (Exception e){ Log.e(MGChargeApplication.TAG, e.getMessage(),e); }
                    }
                }
            }
        } catch (Throwable e) {
            if (e instanceof SocketTimeoutException){
                Log.w(MGChargeApplication.TAG, NameUtil.context()+ " SocketTimeoutException occurred");
            } else {
                Log.e(MGChargeApplication.TAG, NameUtil.context(), e);
            }
        }
        return null;
    }
}
