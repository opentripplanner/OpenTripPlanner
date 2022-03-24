package org.opentripplanner.inspector;

import java.awt.*;

/**
 * Convert a scalar value (bounded or unbounded) to a color.
 * 
 * @author laurent
 */
public interface ScalarColorPalette {

    Color getColor(double value);
}
