package org.opentripplanner.standalone.config.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.ConfigType.BOOLEAN;
import static org.opentripplanner.standalone.config.framework.ConfigType.DOUBLE;
import static org.opentripplanner.standalone.config.framework.ConfigType.DURATION;
import static org.opentripplanner.standalone.config.framework.ConfigType.ENUM;
import static org.opentripplanner.standalone.config.framework.ConfigType.INTEGER;
import static org.opentripplanner.standalone.config.framework.ConfigType.LONG;
import static org.opentripplanner.standalone.config.framework.ConfigType.OBJECT;
import static org.opentripplanner.standalone.config.framework.ConfigType.STRING;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConfigTypeTest {

  @Test
  void description() {
    assertEquals("A decimal floating point _number_. 64 bit.", DOUBLE.description());
  }

  @Test
  void examples() {
    assertEquals("3.15", DOUBLE.examples());
  }

  @Test
  void docName() {
    assertEquals("double", DOUBLE.docName());
  }

  @Test
  void wrap() {
    assertEquals("A", BOOLEAN.wrap("A"));
    assertEquals("\"A\"", DURATION.wrap("A"));
    assertEquals("1", INTEGER.wrap("1"));
    assertEquals("2.2", DOUBLE.wrap("2.2"));
    assertEquals("\"Alf\"", ENUM.wrap("Alf"));
    assertEquals("\"A\"", STRING.wrap("A"));
    assertEquals("2", LONG.wrap("2"));
  }

  @Test
  void isComplex() {
    assertFalse(BOOLEAN.isComplex());
    assertTrue(ConfigType.OBJECT.isComplex());
  }

  @Test
  void isMapOrArray() {
    assertFalse(BOOLEAN.isMapOrArray());
    assertTrue(ConfigType.ARRAY.isMapOrArray());
  }

  @Test
  void of() {
    assertEquals(BOOLEAN, ConfigType.of(Boolean.class));
    assertEquals(DOUBLE, ConfigType.of(Double.class));
    assertEquals(DURATION, ConfigType.of(Duration.class));
    assertEquals(INTEGER, ConfigType.of(Integer.class));
    assertEquals(LONG, ConfigType.of(Long.class));
    assertEquals(STRING, ConfigType.of(String.class));
    assertThrows(IllegalArgumentException.class, () -> ConfigType.of(Object.class));
  }

  @Test
  void getParameter() {
    assertTrue(ConfigType.<Boolean>getParameter(BOOLEAN, BooleanNode.getTrue()));
    assertEquals(2.2, ConfigType.getParameter(DOUBLE, DoubleNode.valueOf(2.2)));
    assertEquals(Duration.ofSeconds(3), ConfigType.getParameter(DURATION, TextNode.valueOf("3s")));
    assertEquals(12, ConfigType.<Integer>getParameter(INTEGER, IntNode.valueOf(12)));
    assertEquals(123L, ConfigType.<Long>getParameter(LONG, LongNode.valueOf(123L)));
    assertEquals("Test", ConfigType.getParameter(STRING, TextNode.valueOf("Test")));
    assertThrows(
      IllegalArgumentException.class,
      () -> ConfigType.getParameter(OBJECT, TextNode.valueOf("Test"))
    );
  }
}
