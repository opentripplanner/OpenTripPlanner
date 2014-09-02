/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
            if (x > 1.0f)
                x = 1.0f;
            return Color.getHSBColor(0.0f, 1.0f, 0.7f - x * 0.7f);
        } else {
            // Green (hue=0.3) to red (hue=1.0) gradient
            float x = (float) ((value - min) / (max - min));
            if (x < 0.0f)
                x = 0.0f;
            return Color.getHSBColor(0.3f + 0.7f * x, 1.0f, 0.7f);
        }
    }
}
