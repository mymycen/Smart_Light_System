package de.iolite.apps.smart_light;

import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.*;
import de.iolite.app.api.device.access.DeviceBooleanProperty.DeviceBooleanPropertyObserver;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.drivers.basic.DriverConstants;
import org.slf4j.Logger;

import java.util.List;

/**
 * Author Massi Wakeli Movement Detection class with detectMovement method
 */

public class Movement {


    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;
    private DeviceLogger dLogger = new DeviceLogger(LOGGER);
    private double blind;
    private double lux = 100;

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

    /**
     * Using movement sensor to detect any movement and checks condition of blinds and luminance values
     * and than turns lights on
     */

    public void detectMovement() {
    	LOGGER.info("detected");
        deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());
        List<Device> deviceList = deviceAPI.getDevices();

        for (Device device : deviceList) {
            if (device.getProfileIdentifier().equals("http://iolite.de#MovementSensor") || device.getIdentifier().contains("bac_livingpantry_move")) {
                DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                if (onProperty != null) {
                	
                	// every movement sensor will be observed
                	onProperty.setObserver(new DeviceBooleanPropertyObserver () {
                    
                
                
                		// if the value of onProperty (movementdetected) changes it will be handled in here
                        @Override 
                        public void valueChanged(Boolean value) {
                        	LOGGER.info("changed");
                        

                            List<Location> roomlist = environmentAPI.getLocations();
                            for (Location location : roomlist) {
                                for (de.iolite.app.api.environment.Device roomDevice : location.getDevices()) {
                                    for (Device deviceControl : deviceList) {
                                    	
                                        // lower then 5 %, it means blinds are nearly completely hidden

                                        if (deviceControl.getProfileIdentifier().equals("http://iolite.de#Blind") && deviceControl.getIdentifier().equals(roomDevice.getIdentifier())) {
                                            DeviceIntegerProperty blindProp = deviceControl.getIntegerProperty(DriverConstants.PROFILE_PROPERTY_Blind_blindLevel_ID);
                                            if (blindProp != null) {
                                                 blind = blindProp.getValue();
                                                LOGGER.info("" + blind);
                                         
                                            }
                                        }


                                        // lower than 500 lux = sunset
                                        if (deviceControl.getProfileIdentifier().equals("http://iolite.de#LuminanceSensor")) {
                                            DeviceDoubleProperty luxProp = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_LuminanceSensor_currentIlluminance_ID);
                                          
                                            if (luxProp != null) {
                                                 lux = luxProp.getValue();
                                        
                                            }


                                        }
                                        
                                        
                                        if (roomDevice.getIdentifier().equals(device.getIdentifier()) && device.getProfileIdentifier().equals(("http://iolite.de#MovementSensor")) && lux < 500 && blind<= 5) {


                                            LOGGER.info("conditions true");

                                            DeviceBooleanProperty onPropertyMove = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
                                            if (onPropertyMove.getValue()) {
                                                for (Device deviceControl2 : deviceList) {

                                                    for (de.iolite.app.api.environment.Device lightdevices : location.getDevices()) {

                                                        if (roomDevice.getLocation()==lightdevices.getLocation()&&deviceControl2.getProfileIdentifier().equals("http://iolite.de#Lamp") && deviceControl2.getIdentifier().equals(lightdevices.getIdentifier())) {
                                                            DeviceBooleanProperty onPropertylight = deviceControl2.getBooleanProperty((DriverConstants.PROFILE_PROPERTY_Lamp_on_ID));

                                                            LOGGER.info(lightdevices.getLocation().getName() +" "+ roomDevice.getLocation().getName());

                                                            if (onPropertylight.getValue() == false && onPropertyMove.getValue() == true) {
                                                                LOGGER.info("Light will be turned on");

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

        }}
    


}
