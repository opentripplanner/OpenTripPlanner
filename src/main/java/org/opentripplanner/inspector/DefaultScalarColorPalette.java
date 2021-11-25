package org.opentripplanner.inspector;

import java.awt.Color;

/**
 * A default ColorPalette with two ranges: min-max-maxMax. It modify the hue between two colors in
 * the first range (here from green to red, via blue), and modify the brightness in the second range
 * (from red to black).
 * 
 * @author laurent
 */
public class DefaultScalarColorPalette implements ScalarColorPalette {

    private double min;

    private double max;

    private double maxMax;

    public DefaultScalarColorPalette(double min, double max, double maxMax) {
        this.min = min;
        this.max = max;
        this.maxMax = maxMax;
    }

    @Override
    public Color getColor(double value) {

        if (value > max) {
            // Red (brightness=0.7) to black (brightness=0.0) gradient
            float x = (float) ((value - max) / (maxMax - max));
            if (x > 1.0f) {
                x = 1.0f;
            }
            return Color.getHSBColor(0.0f, 1.0f, 0.7f - x * 0.7f);
        } else {
            // Green (hue=0.3) to red (hue=1.0) gradient
            float x = (float) ((value - min) / (max - min));
            if (x < 0.0f) {
                x = 0.0f;
            }
            return Color.getHSBColor(0.3f + 0.7f * x, 1.0f, 0.7f);
        }
    }
}
