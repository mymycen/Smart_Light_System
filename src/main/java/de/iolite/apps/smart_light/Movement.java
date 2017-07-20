package de.iolite.apps.smart_light;

import java.util.List;

import de.iolite.api.IOLITEAPINotResolvableException;
import de.iolite.api.IOLITEAPIProvider;
import de.iolite.api.IOLITEPermissionDeniedException;
import de.iolite.api.heating.access.HeatingAPI;
import de.iolite.api.heating.access.PlaceSchedule;
import de.iolite.app.AbstractIOLITEApp;
import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceAPI.DeviceAPIObserver;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.device.access.DeviceBooleanProperty.DeviceBooleanPropertyObserver;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.app.api.device.access.DeviceIntegerProperty;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.app.api.frontend.FrontendAPI;
import de.iolite.app.api.frontend.FrontendAPIException;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.app.api.frontend.util.FrontendAPIUtility;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.app.api.user.access.UserAPI;
import de.iolite.apps.smart_light.internals.PageWithEmbeddedSessionTokenRequestHandler;
import de.iolite.common.lifecycle.exception.CleanUpFailedException;
import de.iolite.common.lifecycle.exception.InitializeFailedException;
import de.iolite.common.lifecycle.exception.StartFailedException;
import de.iolite.common.lifecycle.exception.StopFailedException;
import de.iolite.common.requesthandler.*;
import de.iolite.common.requesthandler.StaticResources.PathHandlerPair;
import de.iolite.drivers.basic.DriverConstants;
import de.iolite.utilities.concurrency.scheduler.Scheduler;
import de.iolite.utilities.disposeable.Disposeable;
import de.iolite.utilities.time.series.DataEntries.AggregatedEntry;
import de.iolite.utilities.time.series.DataEntries.BooleanEntry;
import de.iolite.utilities.time.series.Function;
import de.iolite.utilities.time.series.TimeInterval;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Movement {
	// lumi is true, because there is no luminance sensor and it will stimulated
    boolean lumi = true;
	boolean blinds = false;
	/*
public void activateAutopilot(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI){
	boolean moveDetected = false;
	double temp = 0;
	
	DeviceLogger dLogger = new DeviceLogger(LOGGER);
	deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());
	List <Device> deviceList = deviceAPI.getDevices();
	for (Device device: deviceList){
		
		
		if(device.getProfileIdentifier().equals("http://iolite.de#MovementSensor")){
			DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
			moveDetected = onProperty.getValue();
						
		}
	
	
		if(device.getProfileIdentifier().equals("http://iolite.de#OutdoorTemperatureSensor")){
			DeviceDoubleProperty tempProp = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_OutdoorTemperatureSensor_outsideEnvironmentTemperature_ID);
			temp = tempProp.getValue();
			
						
		}
		
		if(temp<20 & moveDetected){
			if(device.getProfileIdentifier().equals("http://iolite.de#HSVLamp")){
				DeviceDoubleProperty hue = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);
				DeviceDoubleProperty saturation = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);	
				DeviceIntegerProperty dimmingLevel = device.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
				
				try {
					dimmingLevel.requestValueUpdate(Controller.red.getDimmLevel());
					saturation.requestValueUpdate(Controller.red.getSaturation());
					hue.requestValueUpdate(Controller.red.getHue());

					
					
				} catch (DeviceAPIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				
		
			}}
		
		if(temp>20 & moveDetected){
			if(device.getProfileIdentifier().equals("http://iolite.de#HSVLamp")){
				DeviceDoubleProperty hue = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);
				DeviceDoubleProperty saturation = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);	
				DeviceIntegerProperty dimmingLevel = device.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
				
				try {
					dimmingLevel.requestValueUpdate(Controller.blue.getDimmLevel());
					saturation.requestValueUpdate(Controller.blue.getSaturation());
					hue.requestValueUpdate(Controller.blue.getHue());

					
					
				} catch (DeviceAPIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				
		
			}}
	}
	
	/*
	
	
	
	List <Device> deviceList = deviceAPI.getDevices();
	for (Device device: deviceList){
		if(device.getProfileIdentifier().equals("http://iolite.de#LuminanceSensor")){
			DeviceDoubleProperty luminence = device.getDoubleProperty("http://iolite.de#currentIlluminance");
			double lux = luminence.getValue();
			if (lux<5000){
				
				
				
			}
			
			
		}
		
		
		
		
	}
	

	
	

}

*/	
	
public void detectMovement(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI){
 
	DeviceLogger dLogger = new DeviceLogger(LOGGER);
	
	
		deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());

		List <Device> deviceList = deviceAPI.getDevices();
		for (Device device: deviceList){
			if(device.getProfileIdentifier().equals("http://iolite.de#MovementSensor")){
				DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
				if(onProperty!=null){
					
					
					onProperty.setObserver(dLogger.new DeviceOnOffStatusLogger(device.getIdentifier()));
					onProperty.setObserver(new DeviceBooleanPropertyObserver() {
						
						@Override
						public void valueChanged(Boolean arg0) {
							
							
							
							
							Location currentRoom = null;
							List<Location> roomlist = environmentAPI.getLocations();
							for(Location location : roomlist){
								
								for (de.iolite.app.api.environment.Device roomDevice :location.getDevices()){
								
									for(Device deviceControl: deviceList){
										// lower then 5 %, it means blinds are nearly completely hidden
										
										if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Blind")&&deviceControl.getIdentifier().equals(roomDevice.getIdentifier())){
											DeviceIntegerProperty blindProp= deviceControl.getIntegerProperty(DriverConstants.PROFILE_PROPERTY_Blind_blindLevel_ID);
											int blind = blindProp.getValue();
											if(blindProp!=null){
												LOGGER.info(""+blind);
											if (blind <= 5){
												blinds = true;
											}	
										}
										}
										
									
									// lower than 500 lux = sunset
									if(deviceControl.getProfileIdentifier().equals("http://iolite.de#LuminanceSensor")){
										DeviceDoubleProperty luxProp= deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_LuminanceSensor_currentIlluminance_ID);
										double lux = luxProp.getValue();
										if(luxProp!=null){
										if (lux < 500){
											lumi = true;
										}
										
									}
									
									
									}
								if (roomDevice.getIdentifier().equals(device.getIdentifier())&&device.getProfileIdentifier().equals(("http://iolite.de#MovementSensor"))&&lumi&&blinds){
									
										
									LOGGER.info("1");

									 DeviceBooleanProperty onPropertyMove = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
										if (onPropertyMove.getValue()){
									
									
											LOGGER.info("2");

											for(Device deviceControl2: deviceList){
		
										for(de.iolite.app.api.environment.Device lightdevices : location.getDevices()){
										
											if(deviceControl2.getProfileIdentifier().equals("http://iolite.de#Lamp")&&deviceControl2.getIdentifier().equals(lightdevices.getIdentifier())){
												 DeviceBooleanProperty onPropertylight = deviceControl2.getBooleanProperty((DriverConstants.PROFILE_PROPERTY_Lamp_on_ID));
												
													LOGGER.info("3");

												if (onPropertylight.getValue() == false && onPropertyMove.getValue()==true) {
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
					});
					
				}
				
				}
			
		}
	}
		
		
		
	
}
