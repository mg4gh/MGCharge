package mg.charge.control;

import mg.charge.model.Device;
import mg.charge.model.DeviceStatus;

public abstract class DeviceAction {

    Device device;

    public DeviceAction(Device device){
        this.device = device;
    }

    public abstract DeviceStatus execute();
}
