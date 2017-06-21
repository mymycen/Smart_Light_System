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
							LOGGER.info("Movement detected");
							Location currentRoom = null;
							List<Location> roomlist = environmentAPI.getLocations();
							for(Location location : roomlist){
								
								for (de.iolite.app.api.environment.Device roomDevice :location.getDevices()){
									
									for(Device deviceControl: deviceList){
									if (deviceControl.getProfileIdentifier().equals(("http://iolite.de#MovementSensor"))&&deviceControl.getIdentifier().equals(roomDevice.getIdentifier())){
										
										
									final DeviceBooleanProperty onPropertyMove = deviceControl.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
										if (onPropertyMove.getValue()){
									
									

											for(Device deviceControl2: deviceList){
		
										for(de.iolite.app.api.environment.Device lightdevices : location.getDevices()){
										
											if(deviceControl2.getProfileIdentifier().equals("http://iolite.de#Lamp")&&deviceControl2.getIdentifier().equals(lightdevices.getIdentifier())){
												final DeviceBooleanProperty onPropertylight = deviceControl2.getBooleanProperty((DriverConstants.PROPERTY_on_ID));
												
												
												if (onPropertylight.getValue() == false && onPropertyMove.getValue()==true) {
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
