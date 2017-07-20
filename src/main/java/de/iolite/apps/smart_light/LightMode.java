package de.iolite.apps.smart_light;

public class LightMode {

    String modeName;
    int dimmLevel;
    double hue;
    double saturation;

    public LightMode(int dimmLevel, double hue, double saturation, String modeName) {
        this.modeName = modeName;
        this.dimmLevel = dimmLevel;
        this.hue = hue;
        this.saturation = saturation;
    }

    public String getModeName() {
        return modeName;
    }

    public void setModeName(String modeName) {
        this.modeName = modeName;
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
