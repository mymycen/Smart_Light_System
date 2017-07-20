package de.iolite.apps.smart_light;

/**
 * Author Thanh Phuong Siewert
 * Light Mode class with getter and setter
 */

public class LightMode {
	
	int dimmLevel;
	double hue;
	double saturation;
	public LightMode(int dimmLevel, double hue, double saturation){
		this.dimmLevel = dimmLevel;
		this.hue = hue;
		this.saturation = saturation;
	}
	public int getDimmLevel() {
		return dimmLevel;
	}
	public void setDimmLevel(int dimmLevel) {
		this.dimmLevel = dimmLevel;
	}
	public double getHue() {
		return hue;
	}
	public void setHue(double hue) {
		this.hue = hue;
	}
	public double getSaturation() {
		return saturation;
	}
	public void setSaturation(double saturation) {
		this.saturation = saturation;
	}
	
}
