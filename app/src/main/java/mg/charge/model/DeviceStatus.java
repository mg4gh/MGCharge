package mg.charge.model;

public class DeviceStatus {

    boolean connected = false;
    boolean on = false;
    float power = 0;
    long lastRefresh = 0;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public void reset(){
        connected = false;
        on = false;
        power = 0;
    }

    public long getLastRefresh() {
        return lastRefresh;
    }

    public void setLastRefresh(long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    @Override
    public String toString() {
        return "DeviceStatus{" +
                "connected=" + connected +
                ", on=" + on +
                ", power=" + power +
                '}';
    }
}
