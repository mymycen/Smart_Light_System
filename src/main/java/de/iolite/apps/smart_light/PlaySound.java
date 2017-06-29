package de.iolite.apps.smart_light;

import java.io.BufferedInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
public class PlaySound implements LineListener{

	
	private static String ready_audio = "/sounds/ready.WAV";
	private static String livingroom_audio = "/sounds/livingroom.WAV";
	private static String root_audio = "/sounds/root.WAV";
	private static String bedroom_audio = "/sounds/bedroom.WAV";
	private static String kitchen_audio = "/sounds/kitchen.WAV";
	private static String office_audio = "/sounds/office.WAV";
	private static String lighton_audio = "/sounds/lighton.WAV";
	private static String lightoff_audio = "/sounds/lightoff.WAV";
	private static String lightcolorchanged_audio = "/sounds/lightcolorchanged.WAV";
	private static String dontunderstand_audio = "/sounds/dontunderstand.WAV";
	private static String romantic_audio = "/sounds/romantic.WAV";
	private static String party_audio = "/sounds/party.WAV";
	private static String movie_audio = "/sounds/movie.WAV";
	private static String study_audio = "/sounds/study.WAV";
	private static String sleep_audio = "/sounds/sleep.WAV";


	boolean playCompleted;
	Clip clip;

	public PlaySound() {
		super();
	}

	public void playMp3(String context) {

		String filePath = "";
		switch (context) {

		case "READY":

			filePath = ready_audio;

			break;
  
		case "TO_LIVINGROOM":
			filePath = livingroom_audio;

			break;
		case "TO_ROOT":
			filePath = root_audio;

			break;


		case "TO_KITCHEN":
			filePath = kitchen_audio;

			break;

		case "TO_OFFICE":
			filePath = office_audio;

			break;

		case "TO_BEDROOM":
			filePath = bedroom_audio;
			break;
		case "TURN_LIGHT_ON":
			filePath = lighton_audio;

			break;
		case "TURN_LIGHT_OFF":
			filePath = lightoff_audio;

			break;
		case "CHANGE_LIGHT_COLOR":
			filePath = lightcolorchanged_audio;
			break;
		case "DONT_UNDERSTAND":
			filePath = dontunderstand_audio;
			break;
		case "ROMANTIC":
			filePath = romantic_audio;
			break;
		case "PARTY":
			filePath = party_audio;
			break;
		case "SLEEPING":
			filePath = sleep_audio;
			break;
		case "MOVIE":
			filePath = movie_audio;
			break;
		case "STUDYING":
			filePath = study_audio;
			

		}

//		File audioFile = new File(filePath);
		System.out.println(filePath);
		

		try {
			InputStream in = getClass().getResourceAsStream(filePath);
			 InputStream bufferedIn = new BufferedInputStream(in);
			//URL url = getClass().getResource(filePath);
			AudioInputStream audioStream = AudioSystem
					.getAudioInputStream(bufferedIn);

		//	AudioFormat format = audioStream.getFormat();

			//DataLine.Info info = new DataLine.Info(Clip.class, format);

			clip = (Clip) AudioSystem.getClip();

			//clip.addLineListener(this);

			clip.open(audioStream);
			if(clip.isRunning())
				clip.stop();

			clip.start();

//			while (!playCompleted) {
//				// wait for the playback completes
//				try {
//     			Thread.sleep(1000);
//				} catch (InterruptedException ex) {
//					ex.printStackTrace();
//				}
//			}
//
//			audioClip.close();
//			audioStream.close();

		} catch (UnsupportedAudioFileException ex) {
			System.out.println("The specified audio file is not supported.");
			ex.printStackTrace();
		} catch (LineUnavailableException ex) {
			System.out.println("Audio line for playing back is unavailable.");
			ex.printStackTrace();
		} catch (IOException ex) {
			System.out.println("Error playing the audio file.");
			ex.printStackTrace();
		}

	}

	public void update(LineEvent event) {
		LineEvent.Type type = event.getType();

		if (type == LineEvent.Type.START) {
			System.out.println("Playback started.");

		} else if (type == LineEvent.Type.STOP) {
			playCompleted = true;
			System.out.println("Playback completed.");
		}

	}
}
