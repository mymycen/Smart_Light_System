package de.iolite.apps.smart_light;

import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.*;
import de.iolite.app.api.device.access.DeviceBooleanProperty.DeviceBooleanPropertyObserver;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.drivers.basic.DriverConstants;
import org.slf4j.Logger;

import java.util.List;

public class Movement {

    private boolean lumi = true;
    private boolean blinds = false;
    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;
    private DeviceLogger dLogger = new DeviceLogger(LOGGER);

    public Movement(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI) {
        this.deviceAPI = deviceAPI;
        this.environmentAPI = environmentAPI;
        this.LOGGER = LOGGER;
    }

    public void stopDetecting() {
        List<Device> deviceList = deviceAPI.getDevices();
        for (Device device : deviceList) {
            if (device.getProfileIdentifier().equals("http://iolite.de#MovementSensor") || device.getIdentifier().contains("bac_livingpantry_move")) {
                DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                if (onProperty != null) {
                    onProperty.setObserver(null);
                }
            }
        }
    }

    public void detectMovement() {

        deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());
        List<Device> deviceList = deviceAPI.getDevices();

        for (Device device : deviceList) {
            if (device.getProfileIdentifier().equals("http://iolite.de#MovementSensor") || device.getIdentifier().contains("bac_livingpantry_move")) {
                DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                if (onProperty != null) {
                    onProperty.setObserver(dLogger.new DeviceOnOffStatusLogger(device.getIdentifier()));
                    DeviceBooleanPropertyObserver observer = new DeviceBooleanPropertyObserver() {

                        @Override
                        public void valueChanged(Boolean value) {
                            List<Location> roomlist = environmentAPI.getLocations();
                            for (Location location : roomlist) {
                                for (de.iolite.app.api.environment.Device roomDevice : location.getDevices()) {
                                    for (Device deviceControl : deviceList) {
                                        // lower then 5 %, it means blinds are nearly completely hidden

                                        if (deviceControl.getProfileIdentifier().equals("http://iolite.de#Blind") && deviceControl.getIdentifier().equals(roomDevice.getIdentifier())) {
                                            DeviceIntegerProperty blindProp = deviceControl.getIntegerProperty(DriverConstants.PROFILE_PROPERTY_Blind_blindLevel_ID);
                                            if (blindProp != null) {
                                                int blind = blindProp.getValue();
                                                LOGGER.info("" + blind);
                                                if (blind <= 5) {
                                                    blinds = true;
                                                }
                                            }
                                        }


                                        // lower than 500 lux = sunset
                                        if (deviceControl.getProfileIdentifier().equals("http://iolite.de#LuminanceSensor")) {
                                            DeviceDoubleProperty luxProp = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_LuminanceSensor_currentIlluminance_ID);
                                            double lux = luxProp.getValue();
                                            if (luxProp != null) {
                                                if (lux < 500) {
                                                    lumi = true;
                                                }

                                            }


                                        }
                                        if (roomDevice.getIdentifier().equals(device.getIdentifier()) && device.getProfileIdentifier().equals(("http://iolite.de#MovementSensor")) && lumi && blinds) {


                                            LOGGER.info("1");

                                            DeviceBooleanProperty onPropertyMove = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                                            if (onPropertyMove.getValue()) {


                                                LOGGER.info("2");

                                                for (Device deviceControl2 : deviceList) {

                                                    for (de.iolite.app.api.environment.Device lightdevices : location.getDevices()) {

                                                        if (deviceControl2.getProfileIdentifier().equals("http://iolite.de#Lamp") && deviceControl2.getIdentifier().equals(lightdevices.getIdentifier())) {
                                                            DeviceBooleanProperty onPropertylight = deviceControl2.getBooleanProperty((DriverConstants.PROFILE_PROPERTY_Lamp_on_ID));

                                                            LOGGER.info("3");

                                                            if (onPropertylight.getValue() == false && onPropertyMove.getValue() == true) {
                                                                LOGGER.info("4");

                                                                try {
                                                                    onPropertylight.requestValueUpdate(true);
                                                                    LOGGER.info("Light is on in the " + location.getName());

                                                                } catch (DeviceAPIException e) {
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


                        }

                        @Override
                        public void keyChanged(String arg0) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void deviceChanged(Device arg0) {
                            // TODO Auto-generated method stub

                        }
                    };
                    onProperty.setObserver(observer);
                }

            }

        }
    }


}
