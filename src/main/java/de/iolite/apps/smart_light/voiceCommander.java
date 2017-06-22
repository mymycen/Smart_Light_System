package de.iolite.apps.smart_light;

import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.environment.Device;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.drivers.basic.DriverConstants;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import org.slf4j.Logger;

import java.util.List;

/**
 * Created by Leo on 22.06.2017.
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
    private boolean started = false;
    private boolean stopped = true;
    private boolean speechThreadshouldStart = true;
    private boolean speechThreadShouldEnd = false;
    private PlaySound playSound = new PlaySound();
    private static String currentLocation = "root";

    public voiceCommander(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI) {
        this.LOGGER = LOGGER;
        this.deviceAPI = deviceAPI;
        this.environmentAPI = environmentAPI;
        rooms = environmentAPI.getLocations();

        // Configuration
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
    }

    public void stopRecognition() {
        speechThreadShouldEnd = false;
        if (recognizer != null)
            recognizer.stopRecognition();
    }

    public void executeVoiceCommand() {
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

        if (result != null && result.toLowerCase().contains("hello")) {
            started = true;
            stopped = false;
            playSound.playMp3(Controller.Context.WELCOME.toString());
            Thread.sleep(3000);
        } else if (result != null && result.toLowerCase().contains("stop")) {
            stopped = true;
            started = false;

        } else if (result != null && started == true && result.toLowerCase().contains("living room")) {
            for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(Controller.Context.TO_LIVINGROOM.toString());
                    Thread.sleep(3000);
                } else {
                    playSound.playMp3(Controller.Context.ROOM_NOT_EXIST.toString());
                }
            }


        } else if (result != null && started == true && result.toLowerCase().contains("bedroom")) {
            for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(Controller.Context.TO_BEDROOM.toString());
                    Thread.sleep(3000);
                } else {
                    playSound.playMp3(Controller.Context.ROOM_NOT_EXIST.toString());
                }
            }


        } else if (result != null && started == true && result.toLowerCase().contains("kitchen")) {
            for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(Controller.Context.TO_KITCHEN.toString());
                    Thread.sleep(3000);
                } else {
                    playSound.playMp3(Controller.Context.ROOM_NOT_EXIST.toString());
                }
            }


        } else if (result != null && started == true && result.toLowerCase().contains("office")) {
            for (int x = 0; x < rooms.size(); x++) {
                if (result.toLowerCase().contains(rooms.get(x).getName().toLowerCase())) {
                    currentLocation = rooms.get(x).getName();
                    playSound.playMp3(Controller.Context.TO_OFFICE.toString());
                    Thread.sleep(3000);
                } else {
                    playSound.playMp3(Controller.Context.ROOM_NOT_EXIST.toString());
                }

            }


        } else if (result != null && started == true && result.toLowerCase().contains("light")
                && result.toLowerCase().contains("on")) {

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
                playSound.playMp3(Controller.Context.TURN_LIGHT_ON.toString());
                Thread.sleep(3000);
            }

        } else if (result != null && started == true && result.toLowerCase().contains("light")
                && result.toLowerCase().contains("off")) {
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
                playSound.playMp3(Controller.Context.TURN_LIGHT_OFF.toString());
                Thread.sleep(3000);
            }

        } else if (result != null && started == true && result.toLowerCase().contains("change")
                && result.toLowerCase().contains("light") && result.toLowerCase().contains("color")) {

            playSound.playMp3(Controller.Context.CHANGE_LIGHT_COLOR.toString());

        } else if (result != null && started == true) {
            playSound.playMp3(Controller.Context.DONT_UNDERSTAND.toString());
            Thread.sleep(3000);
        }
    }
}
