package org.opentripplanner.model.branding;

public class Presentation {

    private byte[] color;
    private String colorName;
    private byte[] textColor;
    private String textColorName;
    private byte[] backgroundColor;
    private String backgroundColorName;
    private String colorSystem;
    private String textFont;
    private String textFontName;
    private String textLanguage;

    public byte[] getColor() {
        return color;
    }

    public void setColor(byte[] color) {
        this.color = color;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public byte[] getTextColor() {
        return textColor;
    }

    public void setTextColor(byte[] textColor) {
        this.textColor = textColor;
    }

    public String getTextColorName() {
        return textColorName;
    }

    public void setTextColorName(String textColorName) {
        this.textColorName = textColorName;
    }

    public byte[] getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(byte[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBackgroundColorName() {
        return backgroundColorName;
    }

    public void setBackgroundColorName(String backgroundColorName) {
        this.backgroundColorName = backgroundColorName;
    }

    public String getColorSystem() {
        return colorSystem;
    }

    public void setColorSystem(String colorSystem) {
        this.colorSystem = colorSystem;
    }

    public String getTextFont() {
        return textFont;
    }

    public void setTextFont(String textFont) {
        this.textFont = textFont;
    }

    public String getTextFontName() {
        return textFontName;
    }

    public void setTextFontName(String textFontName) {
        this.textFontName = textFontName;
    }

    public String getTextLanguage() {
        return textLanguage;
    }

    public void setTextLanguage(String textLanguage) {
        this.textLanguage = textLanguage;
    }
}
