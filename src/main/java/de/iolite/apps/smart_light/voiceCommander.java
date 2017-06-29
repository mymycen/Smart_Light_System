package de.iolite.apps.smart_light;

import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.app.api.device.access.DeviceIntegerProperty;
import de.iolite.app.api.device.access.DeviceIntegerProperty.DeviceIntegerPropertyObserver;
import de.iolite.app.api.environment.Device;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.drivers.basic.DriverConstants;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import org.slf4j.Logger;

import java.util.List;
import java.util.TimerTask;

import java.util.Timer;

/**
 * Author Thanh Phuong Siewert
 */
public class voiceCommander {

    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;
    private LiveSpeechRecognizer recognizer;
    private String speech;
    private static List<Location> rooms;

    static boolean isLightTurnedon = false;
    static boolean isLightTurnedoff = false;
    static boolean isLightModeChanged = false;
    static boolean isPartyModeOn = false;
    private boolean started = false;
    private boolean stopped = true;
    private boolean speechThreadshouldStart = true;
    private boolean speechThreadShouldEnd = false;
    private PlaySound playSound = new PlaySound();
    private static String currentLocation = "root";
    private Timer timer1;
    private Timer timer2;
    private Timer timer3;
    private Timer timer4;
    
    public enum Commands {
		 TO_ROOT,TO_LIVINGROOM, TO_KITCHEN, TO_OFFICE, TO_BEDROOM, TURN_LIGHT_ON, TURN_LIGHT_OFF, READY, 
		 ROMANTIC, PARTY, SLEEPING, MOVIE, STUDYING};


    public voiceCommander(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI) {
        this.LOGGER = LOGGER;
        this.deviceAPI = deviceAPI;
        this.environmentAPI = environmentAPI;
        rooms = environmentAPI.getLocations();

        // Configuration
        
    }

    public void stopRecognition() {
        speechThreadShouldEnd = false;
        if (recognizer != null)
            recognizer.stopRecognition();
    }

    public void executeVoiceCommand() {
    	Configuration configuration = new Configuration();
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setGrammarPath("resource:/assets/grammars");
        configuration.setGrammarName("grammar");
        configuration.setUseGrammar(true);
        try {
            recognizer = new LiveSpeechRecognizer(configuration);
            LOGGER.info("CREATED BITCH");
        } catch (Exception ex) {
            LOGGER.debug("PROBLEM" + ex.getMessage());
        }
        recognizer.startRecognition(true);
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

        } catch (Exception ex) {
            LOGGER.debug("WARNING", null, ex);
            try {
                if (recognizer != null)
                    recognizer.stopRecognition();
            } catch (Exception e) {
                LOGGER.debug("ERROR", e.getMessage().toString());
            }

        }


    }

    public void processSpeech(String result) throws DeviceAPIException, InterruptedException {

        if (result != null && result.toLowerCase().contains("alice")) {
            started = true;
            stopped = false;
            playSound.playMp3(voiceCommander.Commands.READY.toString());
            
        } else if (result != null && result.toLowerCase().contains("stop")) {
            stopped = true;
            started = false;

        } 
        else if (result != null && started == true && result.toLowerCase().contains("to")&& result.toLowerCase().contains("root")) {
        	stopped = true;
            started = false;
        	
                    currentLocation = "root";
                    playSound.playMp3(voiceCommander.Commands.TO_ROOT.toString());
                    
                
            
    }
        else if (result != null && started == true && result.toLowerCase().contains("living room")) {
        	stopped = true;
            started = false;
        	for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(voiceCommander.Commands.TO_LIVINGROOM.toString());
                    
                } 
            }
            


        } else if (result != null && started == true && result.toLowerCase().contains("bedroom")) {
        	stopped = true;
            started = false;
        	for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(voiceCommander.Commands.TO_BEDROOM.toString());
                    
                }
            }
            


        } else if (result != null && started == true && result.toLowerCase().contains("kitchen")) {
        	stopped = true;
            started = false;

        	for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(voiceCommander.Commands.TO_KITCHEN.toString());
                    
                } 
            }
            

        } else if (result != null && started == true && result.toLowerCase().contains("office")) {
        	stopped = true;
            started = false;
        	for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(voiceCommander.Commands.TO_OFFICE.toString());
                    
                } 

            }
            


        } else if (result != null && started == true && result.toLowerCase().contains("light")
                && result.toLowerCase().contains("on")) {
        	stopped = true;
            started = false;
            if(isPartyModeOn){
            	stopPartyMode();
            	isPartyModeOn = false;
            }

            for (Location location : rooms) {
                if (location.getName().equals(currentLocation)) {
                    List<Device> devices = location.getDevices();

                    for (de.iolite.app.api.environment.Device device : devices) {
                        String deviceIdentifier = device.getIdentifier();
                        for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {
                            if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
                                if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
                                        || deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
                                        || deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
                                    final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    if (onProperty.getValue() == false) {
                                        onProperty.requestValueUpdate(true);
                                        onProperty.setObserver(new DeviceBooleanProperty.DeviceBooleanPropertyObserver() {

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
                                            public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
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

                for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {

                    if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
                            || deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
                            || deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
                        final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                        if (onProperty.getValue() == false) {
                            onProperty.requestValueUpdate(true);
                            onProperty.setObserver(new DeviceBooleanProperty.DeviceBooleanPropertyObserver() {

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
                                public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
                                    // TODO Auto-generated method stub

                                }
                            });

                        }
                    }
                }

            }
            if (isLightTurnedon = true) {
                playSound.playMp3(voiceCommander.Commands.TURN_LIGHT_ON.toString());
            }
            

        } else if (result != null && started == true && result.toLowerCase().contains("light")
                && result.toLowerCase().contains("off")) {
        	stopped = true;
            started = false;
            if(isPartyModeOn){
            	stopPartyMode();
            	isPartyModeOn = false;
            }
            for (Location location : rooms) {
                if (location.getName().equals(currentLocation)) {
                    List<de.iolite.app.api.environment.Device> devices = location.getDevices();

                    for (de.iolite.app.api.environment.Device device : devices) {
                        String deviceIdentifier = device.getIdentifier();
                        for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {
                            if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
                                if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
                                        || deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
                                        || deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
                                    final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    if (onProperty.getValue() == true) {
                                        onProperty.requestValueUpdate(false);
                                        onProperty.setObserver(new DeviceBooleanProperty.DeviceBooleanPropertyObserver() {

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
                                            public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
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

                for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {

                    if (deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")
                            || deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")
                            || deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")) {
                        final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                        if (onProperty.getValue() == true) {
                            onProperty.requestValueUpdate(false);
                            onProperty.setObserver(new DeviceBooleanProperty.DeviceBooleanPropertyObserver() {

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
                                public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
                                    // TODO Auto-generated method stub

                                }
                            });

                        }
                    }
                }

            }
            if (isLightTurnedoff = true) {
                playSound.playMp3(voiceCommander.Commands.TURN_LIGHT_OFF.toString());
            }
            

        } else if (result != null && started == true && result.toLowerCase().contains("romantic")) {

        	started = false;
            stopped = true;
            if(isPartyModeOn){
            	stopPartyMode();
            	isPartyModeOn = false;
            }

            for (Location location : rooms) {
                if (location.getName().equals(currentLocation)) {
                    List<de.iolite.app.api.environment.Device> devices = location.getDevices();

                    for (de.iolite.app.api.environment.Device device : devices) {
                        String deviceIdentifier = device.getIdentifier();
                        for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {
                        	
                            if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
                                if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
//                                    final DeviceBooleanProperty onProperty = deviceControl
//                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    if(onProperty.getValue() == false){
                                    	onProperty.requestValueUpdate(true);
                                    	
                                    }
                                    final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                                    
                                        dimmingLevel.requestValueUpdate(Controller.romaticMode.getDimmLevel());
                                
                                        
                                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                                        hue.requestValueUpdate(Controller.romaticMode.getHue());
                                      
                                        
                                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                                         saturation.requestValueUpdate(Controller.romaticMode.getSaturation());
                                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                                        
                                            
                                            @Override
                                            public void valueChanged(Integer arg0) {
                                                // TODO Auto-generated method stub
                                            	isLightModeChanged = true;
                                            }
                                            
                                            @Override
                                            public void keyChanged(String arg0) {
                                                // TODO Auto-generated method stub
                                                
                                            }
                                            

											@Override
											public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
												// TODO Auto-generated method stub
												
											}
                                        });

                                    
                                }
                                
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                                	final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                	if (onProperty.getValue() == true) {
                                        onProperty.requestValueUpdate(false);
                                	}
                                }
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                                	final DeviceIntegerProperty onProperty = deviceControl
                                            .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                                	onProperty.requestValueUpdate(Controller.romaticMode.getDimmLevel());
                                }


                            }
                        }
                    }

                }

                
            }

            if (currentLocation.equals("root")) {

                for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {

                    if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
                        final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                        if(onProperty.getValue() == false){
                        	onProperty.requestValueUpdate(true);
                        	
                        }

                        final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                        
                        dimmingLevel.requestValueUpdate(Controller.romaticMode.getDimmLevel());
                        
                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                        hue.requestValueUpdate(Controller.romaticMode.getHue());     
                        
                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                         saturation.requestValueUpdate(Controller.romaticMode.getSaturation());
                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                        
                            
                            @Override
                            public void valueChanged(Integer arg0) {
                                // TODO Auto-generated method stub
                                isLightModeChanged = true;
                            }
                            
                            @Override
                            public void keyChanged(String arg0) {
                                // TODO Auto-generated method stub
                                
                            }
                            
                           

							@Override
							public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
								// TODO Auto-generated method stub
								
							}
                        });
                        
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                    	final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                    	if (onProperty.getValue() == true) {
                            onProperty.requestValueUpdate(false);
                    	}
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                    	final DeviceIntegerProperty onProperty = deviceControl
                                .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                    	onProperty.requestValueUpdate(Controller.romaticMode.getDimmLevel());
                    }
                }
                
            }
            if (isLightModeChanged = true) {
                playSound.playMp3(voiceCommander.Commands.ROMANTIC.toString());
                isLightModeChanged = false;
                
            }
            
            
        }else if (result != null && started == true && result.toLowerCase().contains("party")){
        	started = false;
            stopped = true;
            isPartyModeOn = true;
            LOGGER.debug("Party Mode is On");
            TimerTask task1 = new runPartyLight(Controller.partyMode1.getDimmLevel(), Controller.partyMode1.getHue(), Controller.partyMode1.saturation);
            timer1 = new Timer();
            timer1.schedule(task1, 0, 8000);
            
            TimerTask task2 = new runPartyLight(Controller.partyMode2.getDimmLevel(), Controller.partyMode2.getHue(), Controller.partyMode2.saturation);
            timer2 = new Timer();
            timer2.schedule(task2, 2000, 8000);
            
            TimerTask task3 = new runPartyLight(Controller.partyMode3.getDimmLevel(), Controller.partyMode3.getHue(), Controller.partyMode3.saturation);
            timer3 = new Timer();
            timer3.schedule(task3, 4000, 8000);
            
            TimerTask task4 = new runPartyLight(Controller.partyMode4.getDimmLevel(), Controller.partyMode4.getHue(), Controller.partyMode4.saturation);
            timer4 = new Timer();
            timer4.schedule(task4, 6000, 8000);
            
            
            if (isLightModeChanged = true) {
                playSound.playMp3(voiceCommander.Commands.PARTY.toString());
                isLightModeChanged = false;
                
            }
        }
        else if (result != null && started == true && result.toLowerCase().contains("sleeping")){
        	started = false;
            stopped = true;
            if(isPartyModeOn){
            	stopPartyMode();
            	isPartyModeOn = false;
            }

            for (Location location : rooms) {
                if (location.getName().equals(currentLocation)) {
                    List<de.iolite.app.api.environment.Device> devices = location.getDevices();

                    for (de.iolite.app.api.environment.Device device : devices) {
                        String deviceIdentifier = device.getIdentifier();
                        for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {
                        	
                            if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
                                if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
//                                    final DeviceBooleanProperty onProperty = deviceControl
//                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    if(onProperty.getValue() == false){
                                    	onProperty.requestValueUpdate(true);
                                    	
                                    }
                                    final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                                    
                                        dimmingLevel.requestValueUpdate(Controller.sleepingMode.getDimmLevel());
                                
                                        
                                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                                        hue.requestValueUpdate(Controller.sleepingMode.getHue());
                                      
                                        
                                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                                         saturation.requestValueUpdate(Controller.sleepingMode.getSaturation());
                                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                                        
                                            
                                            @Override
                                            public void valueChanged(Integer arg0) {
                                                // TODO Auto-generated method stub
                                            	isLightModeChanged = true;
                                            }
                                            
                                            @Override
                                            public void keyChanged(String arg0) {
                                                // TODO Auto-generated method stub
                                                
                                            }
                                            

											@Override
											public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
												// TODO Auto-generated method stub
												
											}
                                        });

                                    
                                }
                                
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                                	final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                	if (onProperty.getValue() == true) {
                                        onProperty.requestValueUpdate(false);
                                	}
                                }
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                                	final DeviceIntegerProperty onProperty = deviceControl
                                            .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                                	onProperty.requestValueUpdate(Controller.sleepingMode.getDimmLevel());
                                }


                            }
                        }
                    }

                }

                
            }

            if (currentLocation.equals("root")) {

                for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {

                    if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
                        final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                        if(onProperty.getValue() == false){
                        	onProperty.requestValueUpdate(true);
                        	
                        }
                        

                        final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                        
                        dimmingLevel.requestValueUpdate(Controller.sleepingMode.getDimmLevel());
                        
                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                        hue.requestValueUpdate(Controller.sleepingMode.getHue());     
                        
                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                         saturation.requestValueUpdate(Controller.sleepingMode.getSaturation());
                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                        
                            
                            @Override
                            public void valueChanged(Integer arg0) {
                                // TODO Auto-generated method stub
                                isLightModeChanged = true;
                            }
                            
                            @Override
                            public void keyChanged(String arg0) {
                                // TODO Auto-generated method stub
                                
                            }
                            
                           

							@Override
							public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
								// TODO Auto-generated method stub
								
							}
                        });
                        
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                    	final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                    	if (onProperty.getValue() == true) {
                            onProperty.requestValueUpdate(false);
                    	}
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                    	final DeviceIntegerProperty onProperty = deviceControl
                                .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                    	onProperty.requestValueUpdate(Controller.sleepingMode.getDimmLevel());
                    }
                }
                
            }
            if (isLightModeChanged = true) {
                playSound.playMp3(voiceCommander.Commands.SLEEPING.toString());
                isLightModeChanged = false;
                
            }
        }
        else if (result != null && started == true && result.toLowerCase().contains("movie")){
        	started = false;
            stopped = true;
            if(isPartyModeOn){
            	stopPartyMode();
            	isPartyModeOn = false;
            }

            for (Location location : rooms) {
                if (location.getName().equals(currentLocation)) {
                    List<de.iolite.app.api.environment.Device> devices = location.getDevices();

                    for (de.iolite.app.api.environment.Device device : devices) {
                        String deviceIdentifier = device.getIdentifier();
                        for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {
                        	
                            if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
                                if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
//                                    final DeviceBooleanProperty onProperty = deviceControl
//                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    if(onProperty.getValue() == false){
                                    	onProperty.requestValueUpdate(true);
                                    	
                                    }
                                    final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                                    
                                        dimmingLevel.requestValueUpdate(Controller.movieMode.getDimmLevel());
                                
                                        
                                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                                        hue.requestValueUpdate(Controller.movieMode.getHue());
                                      
                                        
                                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                                         saturation.requestValueUpdate(Controller.movieMode.getSaturation());
                                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                                        
                                            
                                            @Override
                                            public void valueChanged(Integer arg0) {
                                                // TODO Auto-generated method stub
                                            	isLightModeChanged = true;
                                            }
                                            
                                            @Override
                                            public void keyChanged(String arg0) {
                                                // TODO Auto-generated method stub
                                                
                                            }
                                            

											@Override
											public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
												// TODO Auto-generated method stub
												
											}
                                        });

                                    
                                }
                                
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                                	final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                	if (onProperty.getValue() == true) {
                                        onProperty.requestValueUpdate(false);
                                	}
                                }
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                                	final DeviceIntegerProperty onProperty = deviceControl
                                            .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                                	onProperty.requestValueUpdate(Controller.movieMode.getDimmLevel());
                                }


                            }
                        }
                    }

                }

                
            }

            if (currentLocation.equals("root")) {

                for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {

                    if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
                        final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                        if(onProperty.getValue() == false){
                        	onProperty.requestValueUpdate(true);
                        	
                        }
                        

                        final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                        
                        dimmingLevel.requestValueUpdate(Controller.movieMode.getDimmLevel());
                        
                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                        hue.requestValueUpdate(Controller.movieMode.getHue());     
                        
                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                         saturation.requestValueUpdate(Controller.movieMode.getSaturation());
                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                        
                            
                            @Override
                            public void valueChanged(Integer arg0) {
                                // TODO Auto-generated method stub
                                isLightModeChanged = true;
                            }
                            
                            @Override
                            public void keyChanged(String arg0) {
                                // TODO Auto-generated method stub
                                
                            }
                            
                           

							@Override
							public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
								// TODO Auto-generated method stub
								
							}
                        });
                        
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                    	final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                    	if (onProperty.getValue() == true) {
                            onProperty.requestValueUpdate(false);
                    	}
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                    	final DeviceIntegerProperty onProperty = deviceControl
                                .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                    	onProperty.requestValueUpdate(Controller.movieMode.getDimmLevel());
                    }
                }
                
            }
            if (isLightModeChanged = true) {
                playSound.playMp3(voiceCommander.Commands.MOVIE.toString());
                isLightModeChanged = false;
                
            }
        }
        else if (result != null && started == true && result.toLowerCase().contains("studying")){
        	started = false;
            stopped = true;
            if(isPartyModeOn){
            	stopPartyMode();
            	isPartyModeOn = false;
            }

            for (Location location : rooms) {
                if (location.getName().equals(currentLocation)) {
                    List<de.iolite.app.api.environment.Device> devices = location.getDevices();

                    for (de.iolite.app.api.environment.Device device : devices) {
                        String deviceIdentifier = device.getIdentifier();
                        for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {
                        	
                            if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
                                if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
                                    final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    if(onProperty.getValue() == false){
                                    	onProperty.requestValueUpdate(true);
                                    	
                                    }
                                    final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                                    
                                        dimmingLevel.requestValueUpdate(Controller.studyingMode.getDimmLevel());
                                
                                        
                                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                                        hue.requestValueUpdate(Controller.studyingMode.getHue());
                                      
                                        
                                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                                         saturation.requestValueUpdate(Controller.studyingMode.getSaturation());
                                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                                        
                                            
                                            @Override
                                            public void valueChanged(Integer arg0) {
                                                // TODO Auto-generated method stub
                                            	isLightModeChanged = true;
                                            }
                                            
                                            @Override
                                            public void keyChanged(String arg0) {
                                                // TODO Auto-generated method stub
                                                
                                            }
                                            

											@Override
											public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
												// TODO Auto-generated method stub
												
											}
                                        });

                                    
                                }
                                
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                                	final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                	if (onProperty.getValue() == false) {
                                        onProperty.requestValueUpdate(true);
                                	}
                                }
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                                	final DeviceIntegerProperty onProperty = deviceControl
                                            .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                                	onProperty.requestValueUpdate(Controller.studyingMode.getDimmLevel());
                                }


                            }
                        }
                    }

                }

                
            }

            if (currentLocation.equals("root")) {

                for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {

                    if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
                        final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                        if(onProperty.getValue() == false){
                        	onProperty.requestValueUpdate(true);
                        	
                        }

                        final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                        
                        dimmingLevel.requestValueUpdate(Controller.studyingMode.getDimmLevel());
                        
                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                        hue.requestValueUpdate(Controller.studyingMode.getHue());     
                        
                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                         saturation.requestValueUpdate(Controller.studyingMode.getSaturation());
                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                        
                            
                            @Override
                            public void valueChanged(Integer arg0) {
                                // TODO Auto-generated method stub
                                isLightModeChanged = true;
                            }
                            
                            @Override
                            public void keyChanged(String arg0) {
                                // TODO Auto-generated method stub
                                
                            }
                            
                           

							@Override
							public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
								// TODO Auto-generated method stub
								
							}
                        });
                        
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                    	final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                    	if (onProperty.getValue() == false) {
                            onProperty.requestValueUpdate(true);
                    	}
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                    	final DeviceIntegerProperty onProperty = deviceControl
                                .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                    	onProperty.requestValueUpdate(Controller.movieMode.getDimmLevel());
                    }
                }
                
            }
            if (isLightModeChanged = true) {
                playSound.playMp3(voiceCommander.Commands.STUDYING.toString());
                isLightModeChanged = false;
                
            }
        }
        
    }
    private class runPartyLight extends TimerTask{
    	int dimmingLevel;
    	double hue;
    	double saturation;
    	
    	public runPartyLight(int dimmingLevel, double hue, double saturation){
    		this.dimmingLevel = dimmingLevel;
    		this.hue = hue;
    		this.saturation = saturation;
    	}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				executePartyLight();
			} catch (DeviceAPIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void executePartyLight() throws DeviceAPIException{
			for (Location location : rooms) {
                if (location.getName().equals(currentLocation)) {
                    List<de.iolite.app.api.environment.Device> devices = location.getDevices();

                    for (de.iolite.app.api.environment.Device device : devices) {
                        String deviceIdentifier = device.getIdentifier();
                        for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {
                        	
                            if (deviceControl.getIdentifier().equals(deviceIdentifier)) {
                                if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
//                                    final DeviceBooleanProperty onProperty = deviceControl
//                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                    if(onProperty.getValue() == false){
                                    	onProperty.requestValueUpdate(true);
                                    	
                                    }
                                    final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                                    
                                        dimmingLevel.requestValueUpdate(this.dimmingLevel);
                                
                                        
                                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                                        hue.requestValueUpdate(this.hue);
                                      
                                        
                                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                                         saturation.requestValueUpdate(this.saturation);
                                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                                        
                                            
                                            @Override
                                            public void valueChanged(Integer arg0) {
                                                // TODO Auto-generated method stub
                                            	isLightModeChanged = true;
                                            }
                                            
                                            @Override
                                            public void keyChanged(String arg0) {
                                                // TODO Auto-generated method stub
                                                
                                            }
                                            

											@Override
											public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
												// TODO Auto-generated method stub
												
											}
                                        });

                                    
                                }
                                
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                                	final DeviceBooleanProperty onProperty = deviceControl
                                            .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                                	if (onProperty.getValue() == true) {
                                        onProperty.requestValueUpdate(false);
                                	}
                                }
                                if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                                	final DeviceIntegerProperty onProperty = deviceControl
                                            .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                                	onProperty.requestValueUpdate(this.dimmingLevel);
                                }


                            }
                        }
                    }

                }

                
            }

            if (currentLocation.equals("root")) {

                for (de.iolite.app.api.device.access.Device deviceControl : deviceAPI.getDevices()) {

                    if (deviceControl.getProfileIdentifier().equals("http://iolite.de#HSVLamp")) {
                        final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                        if(onProperty.getValue() == false){
                        	onProperty.requestValueUpdate(true);
                        	
                        }

                        final DeviceIntegerProperty dimmingLevel = deviceControl.getIntegerProperty(DriverConstants.PROPERTY_dimmingLevel_ID);
                        
                        dimmingLevel.requestValueUpdate(this.dimmingLevel);
                        
                    final DeviceDoubleProperty hue = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);    
                        hue.requestValueUpdate(this.hue);     
                        
                    final DeviceDoubleProperty saturation = deviceControl.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);   
                         saturation.requestValueUpdate(this.saturation);
                         dimmingLevel.setObserver(new DeviceIntegerPropertyObserver() {
                        
                            
                            @Override
                            public void valueChanged(Integer arg0) {
                                // TODO Auto-generated method stub
                                isLightModeChanged = true;
                            }
                            
                            @Override
                            public void keyChanged(String arg0) {
                                // TODO Auto-generated method stub
                                
                            }
                            
                           

							@Override
							public void deviceChanged(de.iolite.app.api.device.access.Device arg0) {
								// TODO Auto-generated method stub
								
							}
                        });
                        
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#Lamp")){
                    	final DeviceBooleanProperty onProperty = deviceControl
                                .getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                    	if (onProperty.getValue() == true) {
                            onProperty.requestValueUpdate(false);
                    	}
                    }
                    if(deviceControl.getProfileIdentifier().equals("http://iolite.de#DimmableLamp")){
                    	final DeviceIntegerProperty onProperty = deviceControl
                                .getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                    	onProperty.requestValueUpdate(this.dimmingLevel);
                    }
                }
                
            }
		}
    	
    }
    private void stopPartyMode(){
    	timer1.cancel();
    	timer2.cancel();
    	timer3.cancel();
    	timer4.cancel();
    	LOGGER.debug("Party Mode is Off");
    	
    }
}
