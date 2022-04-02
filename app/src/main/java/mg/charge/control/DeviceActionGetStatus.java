package mg.charge.control;

import android.util.Log;

import java.net.ProtocolException;
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

public class DeviceActionGetStatus extends DeviceAction{

    long ACTION_RETRY_TIMEOUT = 500;

    public DeviceActionGetStatus(Device device){
        super(device);
    }

    public DeviceStatus execute() {
        long start = System.currentTimeMillis();
        DeviceStatus deviceStatus = new DeviceStatus();
        if (device != null) {
            int cnt = 0;
            Log.d(MGChargeApplication.TAG, NameUtil.context());
            while ((System.currentTimeMillis() - start) < ACTION_RETRY_TIMEOUT) {
                try {
                    cnt++;
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .connectTimeout(3, TimeUnit.SECONDS)
                            .writeTimeout(3, TimeUnit.SECONDS)
                            .readTimeout(3, TimeUnit.SECONDS)
                            .build();

                    String credential = Credentials.basic(device.getUsername(), device.getPassword());
                    String urlPart = "/status";
                    String url = "http://" + device.getIp() + urlPart;
                    Request.Builder requestBuilder = new Request.Builder()
                            .header("Authorization", credential)
                            .url(url);
                    Request request = requestBuilder.build();

                    Call call = client.newCall(request);
                    Response response = call.execute();
                    Log.d(MGChargeApplication.TAG, NameUtil.context() + " response=" + response);

                    if (response.code() == 200) {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String answer = responseBody.string();
                            Log.d(MGChargeApplication.TAG, NameUtil.context() + " body=" + answer);

//                    setConnected(true);
                            deviceStatus.setConnected(true);
                            String[] part = answer.split("\"ison\":");
                            deviceStatus.setOn((part.length == 2) && (part[1].startsWith("true")));

                            try {
                                String power = answer.replaceAll(".*\"power\":", "").replaceFirst(",.*", "");
                                deviceStatus.setPower(Float.parseFloat(power));
                            } catch (NumberFormatException e) {
                                Log.d(MGChargeApplication.TAG, NameUtil.context() + " no power value detected");
                            }
                        }

                    }
                    break;
                } catch (Throwable e) {
                    deviceStatus.reset();

                    if (e instanceof ProtocolException) {
                        Log.w(MGChargeApplication.TAG, NameUtil.context() + " ProtocolException occurred ("+cnt+")");
                    } else if (e instanceof SocketTimeoutException) {
                        Log.w(MGChargeApplication.TAG, NameUtil.context() + " SocketTimeoutException occurred ("+cnt+")");
                    } else {
                        Log.e(MGChargeApplication.TAG, NameUtil.context(), e);
                    }
                }

            }
        }
        Log.d(MGChargeApplication.TAG, NameUtil.context() + " Result: " + deviceStatus);
        return deviceStatus;
    }
}
