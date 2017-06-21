/**
 * Copyright (C) 2016 IOLITE GmbH, All rights reserved.
 */

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

/**
 * <code>ExampleApp</code> is an example IOLITE App.
 *
 * @author Grzegorz Lehmann
 * @author Erdene-Ochir Tuguldur
 * @author Felix Rodemund
 * @since 1.0
 */
public final class Controller extends AbstractIOLITEApp {
	public Controller() {
		// empty
		
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

	/* App APIs */
	private FrontendAPI frontendAPI;
	private StorageAPI storageAPI;
	private static DeviceAPI deviceAPI;
	private static EnvironmentAPI environmentAPI;
	private UserAPI userAPI;

	private HeatingAPI heatingAPI;

	/** front end assets */
	private Disposeable disposeableAssets;

	/**
	 * <code>ExampleApp</code> constructor. An IOLITE App must have a public,
	 * parameter-less constructor.
	 */
	
	private Scheduler scheduler;
	private LiveSpeechRecognizer recognizer;
	private static boolean started = false;
	static boolean stopped = true;
	private String speech;
	protected static List<Location> rooms;
	private static String currentLocation = "root";
	boolean speechThreadshouldStart = false;
	boolean speechThreadShouldEnd = false;
	DeviceLogger dLogger = new DeviceLogger(LOGGER);
	Movement movement = new Movement();


	public enum Context {
		WELCOME, TO_LIVINGROOM, TO_KITCHEN, TO_OFFICE, TO_BEDROOM, TURN_LIGHT_ON, TURN_LIGHT_OFF, CHANGE_LIGHT_COLOR, DONT_UNDERSTAND, ROOM_NOT_EXIST
	};

	private static final class DeviceJSONRequestHandler extends FrontendAPIRequestHandler {

		@Override
		protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
			final JSONArray deviceIdentifiers = new JSONArray();

			final JSONObject response = new JSONObject();
			response.put("identifiers", deviceIdentifiers);
			return new IOLITEHTTPStaticResponse(response.toString(), HTTPStatus.OK,
					IOLITEHTTPResponse.JSON_CONTENT_TYPE);
		}
	}
/*
	private static final class DeviceAddAndRemoveLogger implements DeviceAPIObserver {

		@Override
		public void addedToDevices(final Device device) {
			LOGGER.debug("a new device added '{}'", device.getIdentifier());
		}

		@Override
		public void removedFromDevices(final Device device) {
			LOGGER.debug("a device removed '{}'", device.getIdentifier());
		}
	}

	private static final class DeviceOnOffStatusLogger implements DeviceBooleanPropertyObserver {

		@Nonnull
		private final String identifier;

		private DeviceOnOffStatusLogger(final String deviceIdentifier) {
			this.identifier = Validate.notNull(deviceIdentifier, "'deviceIdentifier' must not be null");
		}

		@Override
		public void deviceChanged(final Device element) {
			// nothing here
		}

		@Override
		public void keyChanged(final String key) {
			// nothing here
		}

		@Override
		public void valueChanged(final Boolean value) {
			if (value) {
				LOGGER.debug("device '{}' turned on", this.identifier);
			} else {
				LOGGER.debug("device '{}' turned off", this.identifier);
			}
		}
	}
*/

	/**
	 * A response handler returning devices filtered by the property type.
	 */
	class DevicesResponseHandler extends FrontendAPIRequestHandler {

		@Override
		protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
			String propertyType;
			try {
				propertyType = new JSONObject(readPassedData(request)).getString("propertyType");
			} catch (final JSONException e) {
				LOGGER.error("Could not handle devices request due to a JSON error: {}", e.getMessage(), e);
				return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
			} catch (final IOException e) {
				LOGGER.error("Could not handle devices request due to an I/O error: {}", e.getMessage(), e);
				return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
			}

			final JSONArray jsonDeviceArray = new JSONArray();
			for (final Device device : deviceAPI.getDevices()) {
				if (device.getProperty(propertyType) != null) {
					// device has the correct property type
					final JSONObject jsonDeviceObject = new JSONObject();
					jsonDeviceObject.put("name", device.getName());
					jsonDeviceObject.put("identifier", device.getIdentifier());
					jsonDeviceArray.put(jsonDeviceObject);
				}
			}

			final JSONObject response = new JSONObject();
			response.put("devices", jsonDeviceArray);
			return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
		}

		private String getCharset(final IOLITEHTTPRequest request) {
			final String charset = request.getCharset();
			return charset == null || charset.length() == 0 ? IOLITEHTTPStaticResponse.ENCODING_UTF8 : charset;
		}

		private String readPassedData(final IOLITEHTTPRequest request) throws IOException {
			final String charset = getCharset(request);
			try (BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(request.getContent(), charset))) {
				final StringBuilder stringBuilder = new StringBuilder();
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					stringBuilder.append(line);
				}
				return stringBuilder.toString();
			}
		}
	}

	/**
	 * A response handler for returning the "not found" response.
	 */
	static class NotFoundResponseHandler extends FrontendAPIRequestHandler {

		@Override
		protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
			return new IOLITEHTTPStaticResponse(HTTPStatus.NotFound, IOLITEHTTPResponse.HTML_CONTENT_TYPE);
		}
	}

	/**
	 * A response handler returning all rooms as JSON array.
	 */
	class RoomsResponseHandler extends FrontendAPIRequestHandler {

		@Override
		protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
			final JSONArray locationNames = new JSONArray();
			for (final Location location : environmentAPI.getLocations()) {
				locationNames.put(location.getName());
			}
			final JSONObject response = new JSONObject();
			response.put("rooms", locationNames);
			return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
		}
	}

	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanUpHook() throws CleanUpFailedException {
		speechThreadShouldEnd=true;
		LOGGER.debug("Cleaning");
		LOGGER.debug("Cleaned");
		
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeHook() throws InitializeFailedException {
		LOGGER.debug("Initializing");
		LOGGER.debug("Initialized");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void startHook(@Nonnull final IOLITEAPIProvider context) throws StartFailedException {
		// here the IOLITE App is started
		// the context gives access to IOLITE App APIs
		LOGGER.debug("Starting");
		
		
		try {
			// use User API
			this.userAPI = context.getAPI(UserAPI.class);
			LOGGER.debug("Running for user '{}' with locale '{}'", this.userAPI.getUser().getIdentifier(),
					this.userAPI.getUser().getLocale());

			// Storage API enables the App to store data persistently
			// whatever is stored via the storage API will also be available if
			// the App is restarted
			this.storageAPI = context.getAPI(StorageAPI.class);
			initializeStorage();

			// Frontend API enables the App to expose a user interface
			this.frontendAPI = context.getAPI(FrontendAPI.class);
			initializeWebResources();

			// Device API gives access to devices connected to IOLITE
			deviceAPI = context.getAPI(DeviceAPI.class);

			// Environment API gives a access for rooms, current situation etc.
			environmentAPI = context.getAPI(EnvironmentAPI.class);
			rooms = environmentAPI.getLocations();

			// Heating API
			this.heatingAPI = context.getAPI(HeatingAPI.class);
			for (final PlaceSchedule placeSchedule : this.heatingAPI.getHeatingSchedulesOfPlaces()) {
				LOGGER.debug("Heating schedule found for place '{}'", placeSchedule.getPlaceIdentifier());
			}
			
		} catch (final IOLITEAPINotResolvableException e) {
			throw new StartFailedException(
					MessageFormat.format("Start failed due to required but not resolvable AppAPI: {0}", e.getMessage()),
					e);
		} catch (final IOLITEPermissionDeniedException e) {
			throw new StartFailedException(MessageFormat
					.format("Start failed due to permission denied problems in the examples: {0}", e.getMessage()), e);
		} catch (final StorageAPIException | FrontendAPIException e) {
			throw new StartFailedException(
					MessageFormat.format("Start failed due to an error in the App API examples: {0}", e.getMessage()),
					e);
		}

		LOGGER.debug("Started");
		movement.detectMovement(LOGGER, deviceAPI, environmentAPI);
		speechThreadshouldStart = false;
	//	executeVoiceCommand();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void stopHook() throws StopFailedException {
		LOGGER.debug("Stopping");
		speechThreadShouldEnd = true;
		if(recognizer!=null)
		recognizer.stopRecognition();

		// deregister the static assets
		if (this.disposeableAssets != null) {
			this.disposeableAssets.dispose();
		}

		LOGGER.debug("Stopped");
	}

	/**
	 * Example method showing how to use the Device API.
	 */
	private void initializeDeviceManager() {
		// register a device observer
		this.deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());

		// go through all devices, and register a property observer for ON/OFF
		// properties
		for (final Device device : this.deviceAPI.getDevices()) {
			// each device has some properties (accessible under
			// device.getProperties())
			// let's get the 'on/off' status property
			final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
			if (onProperty != null) {
				LOGGER.debug("device '{}' has ON/OFF property, current value: '{}'", device.getIdentifier(),
						onProperty.getValue());

				onProperty.setObserver(dLogger.new DeviceOnOffStatusLogger(device.getIdentifier()));
			}
		}

		// go through all devices, and toggle ON/OFF properties
		for (final Device device : this.deviceAPI.getDevices()) {
			// let's get the 'on/off' status property
			final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
			final Boolean onValue;
			if (onProperty != null && (onValue = onProperty.getValue()) != null) {
				LOGGER.debug("toggling device '{}'", device.getIdentifier());
				try {
					onProperty.requestValueUpdate(!onValue);
				} catch (final DeviceAPIException e) {
					LOGGER.error("Failed to control device", e);
				}
			}
		}

		// go through all devices, and print ON/OFF and POWER USAGE property
		// history datas
		for (final Device device : this.deviceAPI.getDevices()) {
			// ON/OFF history data
			final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);

			if (onProperty != null) {
				// retrieve the on/off history of last hour
				final long hourMillis = TimeUnit.SECONDS.toMillis(60 * 60);
				final List<BooleanEntry> onHistory;
				try {
					onHistory = onProperty.getValuesSince(System.currentTimeMillis() - hourMillis);
				} catch (final DeviceAPIException e) {
					LOGGER.error("Failed to retrieve the history of property '{}'", onProperty.getKey(), e);
					continue;
				}
				LOGGER.debug("Got '{}' historical values for property '{}'", onHistory.size(), onProperty.getKey());
				// log history values
				final DateFormat dateFormat = DateFormat.getTimeInstance();
				for (final BooleanEntry historyEntry : onHistory) {
					LOGGER.debug("At '{}' the value was '{}'", dateFormat.format(new Date(historyEntry.time)),
							historyEntry.value);
				}
			}

			// POWER USAGE history data
			final DeviceDoubleProperty powerUsage = device.getDoubleProperty(DriverConstants.PROPERTY_powerUsage_ID);
			if (powerUsage != null) {
				LOGGER.debug("Reading today's hourly power usage data from device '{}':", device.getIdentifier());
				List<AggregatedEntry> history;
				try {
					history = powerUsage.getAggregatedValuesOf(System.currentTimeMillis(), TimeInterval.DAY,
							TimeInterval.HOUR, Function.AVERAGE);
					for (final AggregatedEntry entry : history) {
						LOGGER.debug("The device used an average of {} Watt at '{}'.", entry.getAggregatedValue(),
								DateFormat.getTimeInstance().format(new Date(entry.getEndTime())));
					}
				} catch (final DeviceAPIException e) {
					LOGGER.error("Failed to retrieve history data of device", e);
				}
			}
		}
	}

	/**
	 * Example method showing how to use the Storate API.
	 *
	 * @throws StorageAPIException
	 */
	private void initializeStorage() throws StorageAPIException {
		// basically the Storage API provides a key/value storage for different
		// data types
		// save an integer under the key 'test'
		this.storageAPI.saveInt("test", 10);
		// now let's store a string
		this.storageAPI.saveString("some key", "some value");

		// log the value of an entry, just to demonstrate
		LOGGER.debug("loading 'test' from storage: {}", Integer.valueOf(this.storageAPI.loadInt("test")));
	}

	/**
	 * Registering web resources.
	 *
	 * @throws FrontendAPIException
	 *             if some resources are not found.
	 */
	private final void initializeWebResources() throws FrontendAPIException {

		// go through static assets and register them
		final Map<URI, PathHandlerPair> assets = StaticResources.scanClasspath("assets", getClass().getClassLoader());
		this.disposeableAssets = FrontendAPIUtility.registerPublicHandlers(this.frontendAPI, assets);

		// index page
		final IOLITEHTTPRequestHandler indexPageRequestHandler = new PageWithEmbeddedSessionTokenRequestHandler(
				loadTemplate("assets/index.html"));
		this.frontendAPI.registerRequestHandler("", indexPageRequestHandler);
		this.frontendAPI.registerRequestHandler("index.html", indexPageRequestHandler);

		// default handler returning a not found status
		this.frontendAPI.registerDefaultRequestHandler(new NotFoundResponseHandler());

		// example JSON request handlers
		this.frontendAPI.registerRequestHandler("rooms", new RoomsResponseHandler());
		this.frontendAPI.registerRequestHandler("devices", new DevicesResponseHandler());

		this.frontendAPI.registerRequestHandler("get_devices.json", new DeviceJSONRequestHandler());
	}

	/**
	 * Load a HTML template as string.
	 */
	private String loadTemplate(final String templateResource) {
		try {
			return StaticResources.loadResource(templateResource, getClass().getClassLoader());
		} catch (final IOException e) {
			throw new InitializeFailedException("Loading templates for the dummy app failed", e);
		}
	}
	
	/*
	private void detectMovement(){
		
		LOGGER.info("es läuft");
		
		this.deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());

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
	*/
	private void executeVoiceCommand() {

		LOGGER.debug("INFO", "Loading..\n");
		//this.scheduler = context.getScheduler();

		// Configuration
		Configuration configuration = new Configuration();

		// Load model from the jar
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");

		// if you want to use LanguageModelPath disable the 3 lines after which
		// are setting a custom grammar->

		// configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

		// Grammar
	      configuration.setGrammarPath("resource:/assets/grammars");
		  configuration.setGrammarName("grammar");
		  configuration.setUseGrammar(true);

		try {
			recognizer = new LiveSpeechRecognizer(configuration);
			recognizer.startRecognition(true);
		} catch (Exception ex) {
			LOGGER.debug("PROBLEM" + ex.getMessage());
		}

		// Start the Thread
//		scheduler.execute(startSpeechThread());
		try {
			
			LOGGER.debug("You can start to speak...\n");

			while (speechThreadshouldStart) {
				/*
				 * This method will return when the end of speech is
				 * reached.
				 */
				if (recognizer != null) {
					SpeechResult speechResult = recognizer.getResult();


					if (speechResult != null) {

						speech = speechResult.getHypothesis();
						LOGGER.debug("You said: [" + speech + "]\n");
						processSpeech(speech);

						speech = null;
						speechResult = null;
						//	scheduler.wait(3000);

					} else
						LOGGER.debug("INFO", "I can't understand what you said.\n");


				}
			}
			if(speechThreadShouldEnd){
				if(recognizer!=null)
				recognizer.stopRecognition();
			
				speechThreadshouldStart = false;
			}
			
		} catch (Exception ex) {
			LOGGER.debug("WARNING", null, ex);
			try{
				if(recognizer!=null)
				recognizer.stopRecognition();
			}
			catch(Exception e){
				LOGGER.debug("ERROR",e.getMessage().toString());
			}
          
		}

	
		
		
	
		

	}

//	protected Runnable startSpeechThread() {
//		class speechThread implements Runnable {
//   
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				try {
//					
//					LOGGER.debug("INFO", "You can start to speak...\n");
//
//					while (speechThreadshouldStart) {
//						/*
//						 * This method will return when the end of speech is
//						 * reached.
//						 */
//						SpeechResult speechResult = recognizer.getResult();
//						if (speechResult != null) {
//
//							speech = speechResult.getHypothesis();
//							LOGGER.debug("You said: [" + speech + "]\n");
//							processSpeech(speech);
//
//							speech = null;
//							speechResult = null;
//							scheduler.wait(3000);
//
//						} else
//							LOGGER.debug("INFO", "I can't understand what you said.\n");
//
//					}
//					if(speechThreadShouldEnd){
//						recognizer.stopRecognition();
//					
//						speechThreadshouldStart = false;
//					}
//					
//				} catch (Exception ex) {
//					LOGGER.debug("WARNING", null, ex);
//					try{
//						recognizer.stopRecognition();
//					}
//					catch(Exception e){
//						LOGGER.debug("ERROR",e.getMessage().toString());
//					}
//	
//				}
//
//				LOGGER.debug("INFO", "SpeechThread has exited...");
//				
//
//			}
//
//		}
//		return new speechThread();
//
//	}

	private static PlaySound playSound = new PlaySound();
	static boolean isLightTurnedon = false;
	static boolean isLightTurnedoff = false;

	public static void processSpeech(String result) throws DeviceAPIException, InterruptedException {

		if (result != null && result.toLowerCase().contains("hello")) {
			started = true;
			stopped = false;
			playSound.playMp3(Context.WELCOME.toString());
			Thread.sleep(3000);
		}

		else if (result != null && result.toLowerCase().contains("stop")) {
			stopped = true;
			started = false;

		}

		else if (result != null && started == true && result.toLowerCase().contains("living room")) {
			for (int x = 0; x < rooms.size(); x++) {
				if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
					currentLocation = rooms.get(x).getName();
					playSound.playMp3(Context.TO_LIVINGROOM.toString());
					Thread.sleep(3000);
				}
				else{
					playSound.playMp3(Context.ROOM_NOT_EXIST.toString());
				}
			}

			
			

		}

		else if (result != null && started == true && result.toLowerCase().contains("bedroom")) {
			for (int x = 0; x < rooms.size(); x++) {
				if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
					currentLocation = rooms.get(x).getName();
					playSound.playMp3(Context.TO_BEDROOM.toString());
					Thread.sleep(3000);
				}
				else{
					playSound.playMp3(Context.ROOM_NOT_EXIST.toString());
				}
			}
			

		} else if (result != null && started == true && result.toLowerCase().contains("kitchen")) {
			for (int x = 0; x < rooms.size(); x++) {
				if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
					currentLocation = rooms.get(x).getName();
					playSound.playMp3(Context.TO_KITCHEN.toString());
					Thread.sleep(3000);
				}
				else{
					playSound.playMp3(Context.ROOM_NOT_EXIST.toString());
				}
			}
			

		} else if (result != null && started == true && result.toLowerCase().contains("office")) {
			for (int x = 0; x < rooms.size(); x++) {
				if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
					currentLocation = rooms.get(x).getName();
					playSound.playMp3(Context.TO_OFFICE.toString());
					Thread.sleep(3000);
				}
				else{
					playSound.playMp3(Context.ROOM_NOT_EXIST.toString());
				}

			}
			

		} else if (result != null && started == true && result.toLowerCase().contains("light")
				&& result.toLowerCase().contains("on")) {

			for (Location location : rooms) {
				if (location.getName().equals(currentLocation)) {
					List<de.iolite.app.api.environment.Device> devices = location.getDevices();

					for (de.iolite.app.api.environment.Device device : devices) {
						String deviceIdentifier = device.getIdentifier();
						for (Device deviceControl : deviceAPI.getDevices()) {
							if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
								if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
										|| deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
										|| deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
									final DeviceBooleanProperty onProperty = deviceControl
											.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
									if (onProperty.getValue() == false) {
										onProperty.requestValueUpdate(true);
										onProperty.setObserver(new DeviceBooleanPropertyObserver() {

											@Override
											public void valueChanged(Boolean arg0) {
												// TODO Auto-generated method
												// stub
												isLightTurnedon = true;

											}

											@Override
											public void keyChanged(String arg0) {
												// TODO Auto-generated method
												// stub

											}

											@Override
											public void deviceChanged(Device arg0) {
												// TODO Auto-generated method
												// stub

											}
										});

									}
								}
							}
						}
					}

				}

				
			}

			if (currentLocation.equals("root")) {

				for (Device deviceControl : deviceAPI.getDevices()) {

					if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
							|| deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
							|| deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
						final DeviceBooleanProperty onProperty = deviceControl
								.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
						if (onProperty.getValue() == false) {
							onProperty.requestValueUpdate(true);
							onProperty.setObserver(new DeviceBooleanPropertyObserver() {

								@Override
								public void valueChanged(Boolean arg0) {
									// TODO Auto-generated method stub
									isLightTurnedon = true;

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
			if (isLightTurnedon = true) {
				playSound.playMp3(Context.TURN_LIGHT_ON.toString());
				Thread.sleep(3000);
			}

		} else if (result != null && started == true && result.toLowerCase().contains("light")
				&& result.toLowerCase().contains("off")) {
			for (Location location : rooms) {
				if (location.getName().equals(currentLocation)) {
					List<de.iolite.app.api.environment.Device> devices = location.getDevices();

					for (de.iolite.app.api.environment.Device device : devices) {
						String deviceIdentifier = device.getIdentifier();
						for (Device deviceControl : deviceAPI.getDevices()) {
							if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
								if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
										|| deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
										|| deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
									final DeviceBooleanProperty onProperty = deviceControl
											.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
									if (onProperty.getValue() == true) {
										onProperty.requestValueUpdate(false);
										onProperty.setObserver(new DeviceBooleanPropertyObserver() {

											@Override
											public void valueChanged(Boolean arg0) {
												// TODO Auto-generated method
												// stub
												isLightTurnedoff = true;

											}

											@Override
											public void keyChanged(String arg0) {
												// TODO Auto-generated method
												// stub

											}

											@Override
											public void deviceChanged(Device arg0) {
												// TODO Auto-generated method
												// stub

											}
										});

									}
								}
							}
						}
					}

				}

				
			}

			if (currentLocation.equals("root")) {

				for (Device deviceControl : deviceAPI.getDevices()) {

					if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
							|| deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
							|| deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
						final DeviceBooleanProperty onProperty = deviceControl
								.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
						if (onProperty.getValue() == true) {
							onProperty.requestValueUpdate(false);
							onProperty.setObserver(new DeviceBooleanPropertyObserver() {

								@Override
								public void valueChanged(Boolean arg0) {
									// TODO Auto-generated method stub
									isLightTurnedoff = true;

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
			if (isLightTurnedoff = true) {
				playSound.playMp3(Context.TURN_LIGHT_OFF.toString());
				Thread.sleep(3000);
			}

		} else if (result != null && started == true && result.toLowerCase().contains("change")
				&& result.toLowerCase().contains("light") && result.toLowerCase().contains("color")) {

			playSound.playMp3(Context.CHANGE_LIGHT_COLOR.toString());

		} else if (result != null && started == true) {
			playSound.playMp3(Context.DONT_UNDERSTAND.toString());
			Thread.sleep(3000);
		}
	}

}