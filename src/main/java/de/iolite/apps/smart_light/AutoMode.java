package de.iolite.apps.smart_light;


import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.device.access.DeviceBooleanProperty.DeviceBooleanPropertyObserver;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.drivers.basic.DriverConstants;
import org.slf4j.Logger;

import java.util.List;
/**
 * Author Massi Wakeli 
 */
public class AutoMode {

    //public LightMode hot = new LightMode (1,100,85,"hot");
    //public LightMode cool = new LightMode (208,100,85,"cool");
    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;

    public AutoMode(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI) {
        this.deviceAPI = deviceAPI;
        this.LOGGER = LOGGER;
        this.environmentAPI = environmentAPI;
    }

    public void stopAutopilot() {
        List<Device> deviceList = deviceAPI.getDevices();
        for (Device device : deviceList) {
            if (device.getProfileIdentifier().equals("http://iolite.de#MovementSensor")) {
                DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                if (onProperty != null) {
                    onProperty.setObserver(null);
                }
            }
        }
    }


    public void activateAutopilot() {

        DeviceLogger dLogger = new DeviceLogger(LOGGER);
        deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());
        List<Device> deviceList = deviceAPI.getDevices();
        for (Device device : deviceList) {
            if (device.getProfileIdentifier().equals("http://iolite.de#MovementSensor")) {
                DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                if (onProperty != null) {

                    onProperty.setObserver(dLogger.new DeviceOnOffStatusLogger(device.getIdentifier()));
                    onProperty.setObserver(new DeviceBooleanPropertyObserver() {

                        @Override
                        public void valueChanged(Boolean arg0) {

                            DeviceBooleanProperty onPropertyMove = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                            if (onPropertyMove != null) {
                                if (onPropertyMove.getValue()) {

                                    for (Device device2 : deviceList) {
                                        if (device2.getProfileIdentifier().equals("http://iolite.de#OutdoorTemperatureSensor")) {
                                            DeviceDoubleProperty tempProp = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_OutdoorTemperatureSensor_outsideEnvironmentTemperature_ID);
                                            Double temp = tempProp.getValue();
                                            if (tempProp != null) {


                                                if (temp > 20) {
                                                    for (Device dimm : deviceList) {
                                                        if (dimm.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {

                                                            DeviceDoubleProperty hue = dimm.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);
                                                            DeviceDoubleProperty saturation = dimm.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);
                                                            DeviceDoubleProperty dimmingLevel = dimm.getDoubleProperty(DriverConstants.PROPERTY_dimmingLevel_ID);

                                                            try {
                                                                dimmingLevel.requestValueUpdate(100.0);
                                                                saturation.requestValueUpdate(85.0);
                                                                hue.requestValueUpdate(208.0);


                                                            } catch (DeviceAPIException e) {
                                                                // TODO Auto-generated catch block
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                }
                                                if (temp <= 20) {
                                                    for (Device dimm : deviceList) {
                                                        if (dimm.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {

                                                            DeviceDoubleProperty hue = dimm.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);
                                                            DeviceDoubleProperty saturation = dimm.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);
                                                            DeviceDoubleProperty dimmingLevel = dimm.getDoubleProperty(DriverConstants.PROPERTY_dimmingLevel_ID);

                                                            try {
                                                                dimmingLevel.requestValueUpdate(100.0);
                                                                saturation.requestValueUpdate(85.0);
                                                                hue.requestValueUpdate(1.0);


                                                            } catch (DeviceAPIException e) {
                                                                // TODO Auto-generated catch block
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void keyChanged(String arg0) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void deviceChanged(Device arg0) {
                            // TODO Auto-generated method stub

                        }
                    });
                }
            }
        }
    }
}