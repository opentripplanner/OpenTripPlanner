package org.opentripplanner.utils.color;

import java.awt.Color;

public final class ColorUtils {

  private ColorUtils() {}

  /**
   * Calculates luminance according to
   * <a href="https://www.w3.org/TR/WCAG21/#dfn-relative-luminance">W3C Recommendation</a>
   */
  public static double computeLuminance(Color color) {
    //gets float of RED, GREEN, BLUE in range 0...1
    float[] colorComponents = color.getRGBColorComponents(null);
    double r = linearizeColorComponent(colorComponents[0]);
    double g = linearizeColorComponent(colorComponents[1]);
    double b = linearizeColorComponent(colorComponents[2]);
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  }

  private static double linearizeColorComponent(double srgb) {
    return srgb <= 0.04045 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
  }

  /**
   * Determine if a color is light or dark
   * <p>
   * A light color is a color where the contrast ratio with black is larger than with white.
   * <p>
   * The contrast ratio is defined per Web Content Accessibility Guidelines (WCAG) 2.1.
   */
  public static Brightness computeBrightness(Color color) {
    // The contrast ratio between two colors is defined as (L1 + 0.05) / (L2 + 0.05)
    // where L1 is the lighter of the two colors.
    //
    // Therefore, the contrast ratio with black is (L + 0.05) / 0.05 and the contrast ratio with
    // white is 1.05 / (L + 0.05)
    //
    // Solving (L + 0.05) / 0.05 > 1.05 / (L + 0.05) gets L > 0.179
    return computeLuminance(color) > 0.179 ? Brightness.LIGHT : Brightness.DARK;
  }
}
