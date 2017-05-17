package java_classes;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

public class SpeechtoString {

	
	// Logger
	private Logger logger = Logger.getLogger(getClass().getName());
	static boolean started = false;
	static boolean stopped = true;

	// Variables
	private static String result;

	// Threads
	Thread	speechThread;
	Thread	resourcesThread;

	// LiveRecognizer
	private LiveSpeechRecognizer recognizer;
	
	public enum Context{WELCOME,TO_LIVINGROOM,TO_KITCHEN,TO_OFFICE,TO_BEDROOM,TURN_LIGHT_ON,TURN_LIGHT_OFF,CHANGE_LIGHT_COLOR,DONT_UNDERSTAND};

	/**
	 * Constructor
	 */
	public SpeechtoString() {

		// Loading Message
		logger.log(Level.INFO, "Loading..\n");

		// Configuration
		Configuration configuration = new Configuration();

		// Load model from the jar
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");

		// if you want to use LanguageModelPath disable the 3 lines after which
		// are setting a custom grammar->

		// configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")

		// Grammar
		configuration.setGrammarPath("resources/grammars");
		configuration.setGrammarName("grammar");
		configuration.setUseGrammar(true);

		try {
			recognizer = new LiveSpeechRecognizer(configuration);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		// Start recognition process pruning previously cached data.
		recognizer.startRecognition(true);

		// Start the Thread
		startSpeechThread();
		
	}

	/**
	 * Starting the main Thread of speech recognition
	 */
	protected void startSpeechThread() {

	
		
		speechThread = new Thread(() -> {
			logger.log(Level.INFO, "You can start to speak...\n");
			try {
				while (true) {
					/*
					 * This method will return when the end of speech is
					 * reached. 
					 */
					SpeechResult speechResult = recognizer.getResult();
					if (speechResult != null) {

						result = speechResult.getHypothesis();
						System.out.println("You said: [" + result + "]\n");
						processSpeech(result);
						
						result = null;
						speechResult = null;
						
						
						

					} else
						logger.log(Level.INFO, "I can't understand what you said.\n");

				}
			} catch (Exception ex) {
				logger.log(Level.WARNING, null, ex);
			}

			logger.log(Level.INFO, "SpeechThread has exited...");
		});

		// Start
		speechThread.start();

	}



	/**
	 * Java Main Application Method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// // Be sure that the user can't start this application by not giving
		// the
		// // correct entry string
		// if (args.length == 1 && "SPEECH".equalsIgnoreCase(args[0]))
		new SpeechtoString();
		
		// else
		// Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Give me
		// the correct entry string..");

	}
	
	private static PlaySound playSound = new PlaySound();
	
	public static void processSpeech(String result){
		if(result != null && result.toLowerCase().contains("ready")){
			started = true;
			stopped = false;
			playSound.playMp3(Context.WELCOME.toString());
		}
		
		else if(result != null && result.toLowerCase().contains("stop")){
			stopped = true;
			started = false;
			
		}
		
		else if (result != null && started == true &&result.toLowerCase().contains("living room")){
			playSound.playMp3(Context.TO_LIVINGROOM.toString());
			
		}
		
		else if (result != null && started == true&& result.toLowerCase().contains("bedroom")){
			playSound.playMp3(Context.TO_BEDROOM.toString());
			
		}
		else if (result != null  && started == true && result.toLowerCase().contains("kitchen")){
			playSound.playMp3(Context.TO_KITCHEN.toString());
			
		}
		else if (result != null && started == true&&result.toLowerCase().contains("office")){
			playSound.playMp3(Context.TO_OFFICE.toString());
			
		}
		else if (result != null && started == true&& result.toLowerCase().contains("turn") && result.toLowerCase().contains("light")&& result.toLowerCase().contains("on")){
			playSound.playMp3(Context.TURN_LIGHT_ON.toString());
			
		}
		else if (result != null && started == true&&result.toLowerCase().contains("turn") && result.toLowerCase().contains("light")&& result.toLowerCase().contains("off")){
			playSound.playMp3(Context.TURN_LIGHT_OFF.toString());
			
		}
		else if (result != null && started == true &&result.toLowerCase().contains("change") && result.toLowerCase().contains("light")&& result.toLowerCase().contains("color")){
			playSound.playMp3(Context.CHANGE_LIGHT_COLOR.toString());
			
		}
		else if(result != null && started == true){
			playSound.playMp3(Context.DONT_UNDERSTAND.toString());
		}
	}

}
