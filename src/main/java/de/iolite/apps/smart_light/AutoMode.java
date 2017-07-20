package de.iolite.apps.smart_light;


import de.iolite.api.IOLITEAPINotResolvableException;
import de.iolite.api.IOLITEAPIProvider;
import de.iolite.api.IOLITEPermissionDeniedException;
import de.iolite.api.heating.access.HeatingAPI;
import de.iolite.api.heating.access.PlaceSchedule;
import de.iolite.app.AbstractIOLITEApp;
import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.app.api.device.access.DeviceBooleanProperty.DeviceBooleanPropertyObserver;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.app.api.frontend.FrontendAPI;
import de.iolite.app.api.frontend.FrontendAPIException;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.app.api.frontend.util.FrontendAPIUtility;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.app.api.user.access.UserAPI;
import de.iolite.apps.smart_light.DeviceLogger.DeviceAddAndRemoveLogger;
import de.iolite.apps.smart_light.DeviceLogger.DeviceOnOffStatusLogger;
import de.iolite.apps.smart_light.RequestHandlers.*;
import de.iolite.apps.smart_light.internals.PageWithEmbeddedSessionTokenRequestHandler;
import de.iolite.common.lifecycle.exception.CleanUpFailedException;
import de.iolite.common.lifecycle.exception.InitializeFailedException;
import de.iolite.common.lifecycle.exception.StartFailedException;
import de.iolite.common.lifecycle.exception.StopFailedException;
import de.iolite.common.requesthandler.*;
import de.iolite.common.requesthandler.StaticResources.PathHandlerPair;
import de.iolite.drivers.basic.DriverConstants;
import de.iolite.utilities.disposeable.Disposeable;
import de.iolite.utilities.time.series.DataEntries.AggregatedEntry;
import de.iolite.utilities.time.series.DataEntries.BooleanEntry;
import de.iolite.utilities.time.series.Function;
import de.iolite.utilities.time.series.TimeInterval;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AutoMode {
	
	//public LightMode hot = new LightMode (1,100,85,"hot");
	//public LightMode cool = new LightMode (208,100,85,"cool");

	
	
	public void activateAutopilot(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI){

	
		
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

								 DeviceBooleanProperty onPropertyMove = device.getBooleanProperty(DriverConstants.PROFILE_PROPERTY_MovementSensor_movementDetected_ID);
								if(onPropertyMove!=null){	
								 if (onPropertyMove.getValue()){
								
										for (Device device2: deviceList){
											if(device2.getProfileIdentifier().equals("http://iolite.de#OutdoorTemperatureSensor")){
										DeviceDoubleProperty tempProp = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_OutdoorTemperatureSensor_outsideEnvironmentTemperature_ID);
										Double temp = tempProp.getValue();
										if(tempProp!=null){
											
											
											
										if(temp>20){
											for (Device dimm: deviceList){
												if(dimm.getProfileIdentifier().equals("http://iolite.de#HSVLamp")){
													
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
										if(temp<=20){	for (Device dimm: deviceList){
											if(dimm.getProfileIdentifier().equals("http://iolite.de#HSVLamp")){
												
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
}}}