package de.iolite.apps.smart_light;

import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.drivers.basic.DriverConstants;
import org.slf4j.Logger;

import java.util.List;

/**
 * Created by Leo on 20.07.2017.
 */
public class Movement2 {

    public void detectMovement(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI) {

        DeviceLogger dLogger = new DeviceLogger(LOGGER);


        deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());

        List<Device> deviceList = deviceAPI.getDevices();
        for (Device device : deviceList)
            if (device.getProfileIdentifier().equals("http://iolite.de#MovementSensor")) {
                DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                if (onProperty != null) {


                    onProperty.setObserver(dLogger.new DeviceOnOffStatusLogger(device.getIdentifier()));
                    onProperty.setObserver(new DeviceBooleanProperty.DeviceBooleanPropertyObserver() {

                        @Override
                        public void valueChanged(Boolean arg0) {
                            LOGGER.info("Movement detected");
                            Location currentRoom = null;
                            List<Location> roomlist = environmentAPI.getLocations();
                            for (Location location : roomlist) {

                                for (de.iolite.app.api.environment.Device roomDevice : location.getDevices()) {

                                    for (Device deviceControl : deviceList) {
                                        if (deviceControl.getProfileIdentifier().equals(("http://iolite.de#MovementSensor")) && deviceControl.getIdentifier().equals(roomDevice.getIdentifier())) {


                                            final DeviceBooleanProperty onPropertyMove = deviceControl.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                                            if (onPropertyMove.getValue()) {


                                                for (Device deviceControl2 : deviceList) {

                                                    for (de.iolite.app.api.environment.Device lightdevices : location.getDevices()) {

                                                        if (deviceControl2.getProfileIdentifier().equals("http://iolite.de#Lamp") && deviceControl2.getIdentifier().equals(lightdevices.getIdentifier())) {
                                                            final DeviceBooleanProperty onPropertylight = deviceControl2.getBooleanProperty((DriverConstants.PROPERTY_on_ID));


                                                            if (onPropertylight.getValue() == false && onPropertyMove.getValue() == true) {
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
                    });

                }

            }
    }


}

