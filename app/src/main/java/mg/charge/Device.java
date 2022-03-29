package mg.charge;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

public class Device {

    String id = UUID.randomUUID().toString();
    String name = "";
    String ip = "";
    String username = "";
    String password = "";

    public Device(){
    }

    public Device(String id, String name, String ip, String username, String password){
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public void save(File devFile){
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(devFile));
            pw.println("id="+id);
            pw.println("name="+name);
            pw.println("ip="+ip);
            pw.println("username="+username);
            pw.println("password="+password);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Device load(File devFile){
        Device device = new Device();
        try {
            BufferedReader in = new BufferedReader(new FileReader(devFile));
            String line;
            while ((line=in.readLine()) != null){
                if (line.startsWith("id=")) device.id = line.replaceFirst("id=", "");
                if (line.startsWith("name=")) device.name = line.replaceFirst("name=", "");
                if (line.startsWith("ip=")) device.ip = line.replaceFirst("ip=", "");
                if (line.startsWith("username=")) device.username = line.replaceFirst("username=", "");
                if (line.startsWith("password=")) device.password = line.replaceFirst("password=", "");
            }
        } catch (Exception e) {
            Log.e(MGChargeApplication.TAG, NameUtil.context(), e);
            device = null;
        }
        return device;
    }

    @NonNull
    @Override
    public String toString() {
        return getName()+" ("+getIp()+")";
    }
}
