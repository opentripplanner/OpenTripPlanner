package org.opentripplanner.utils.color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.opentripplanner.utils.color.ColorUtils.computeBrightness;

import java.awt.Color;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

public class ColorUtilsTest {

  static final Arguments[] brightnessExpectations = {
    arguments(Color.black, Brightness.DARK),
    arguments(Color.green, Brightness.LIGHT),
    arguments(Color.blue, Brightness.DARK),
    arguments(Color.red, Brightness.LIGHT),
    arguments(Color.yellow, Brightness.LIGHT),
    arguments(Color.white, Brightness.LIGHT),
    arguments(Color.pink, Brightness.LIGHT),
    arguments(Color.orange, Brightness.LIGHT),
    arguments(Color.cyan, Brightness.LIGHT),
  };

  @ParameterizedTest
  @FieldSource("brightnessExpectations")
  void testBrightness(Color color, Brightness brightness) {
    assertEquals(computeBrightness(color), brightness);
  }
}
