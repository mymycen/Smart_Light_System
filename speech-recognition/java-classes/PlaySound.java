package java_classes;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class PlaySound implements LineListener {

	private String lightTurnedoff = "lightTurnedoff";
	private String lightTurnedon = "lightTurnedon";
	private static String welcome_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/welcome.wav";
	private static String livingroom_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/livingroom.wav";
	private static String bedroom_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/bedroom.wav";
	private static String kitchen_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/kitchen.wav";
	private static String office_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/office.wav";
	private static String lighton_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/lighton.wav";
	private static String lightoff_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/lightoff.wav";
	private static String lightcolorchanged_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/lightcolorchanged.wav";
	private static String dontunderstand_audio = "C:/Users/ThanhPhuong/work/IOLITE_SmartLightControlSystem/resources/sounds/dontunderstand.wav";
	boolean playCompleted;

	public PlaySound() {
		super();
	}

	public void playMp3(String context) {

		String filePath = "";
		switch (context) {

		case "WELCOMEEEE":

			filePath = welcome_audio;

			break;

		case "TO_LIVINGROOM":
			filePath = livingroom_audio;

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

		}

		File audioFile = new File(filePath);
		System.out.println(filePath);

		try {
			AudioInputStream audioStream = AudioSystem
					.getAudioInputStream(audioFile);

			AudioFormat format = audioStream.getFormat();

			DataLine.Info info = new DataLine.Info(Clip.class, format);

			Clip audioClip = (Clip) AudioSystem.getLine(info);

			audioClip.addLineListener(this);

			audioClip.open(audioStream);

			audioClip.start();

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
