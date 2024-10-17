package org.opentripplanner.standalone.config.framework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.BOOLEAN;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.DOUBLE;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.DURATION;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.INTEGER;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.LONG;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.OBJECT;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.STRING;

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
  void examplesToMarkdown() {
    assertEquals("`3.15`", DOUBLE.examplesToMarkdown());
    assertEquals("`\"This is a string!\"`", STRING.examplesToMarkdown());
  }

  @Test
  void docName() {
    assertEquals("double", DOUBLE.docName());
  }

  @Test
  void wrap() {
    assertEquals("A", BOOLEAN.quote("A"));
    assertEquals("\"A\"", DURATION.quote("A"));
    assertEquals("1", INTEGER.quote("1"));
    assertEquals("2.2", DOUBLE.quote("2.2"));
    assertEquals("\"Alf\"", ENUM.quote("Alf"));
    assertEquals("\"A\"", STRING.quote("A"));
    assertEquals("2", LONG.quote("2"));
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
    assertTrue(BOOLEAN.<Boolean>valueOf(BooleanNode.getTrue()));
    assertEquals(2.2, DOUBLE.valueOf(DoubleNode.valueOf(2.2)));
    assertEquals(Duration.ofSeconds(3), DURATION.valueOf(TextNode.valueOf("3s")));
    assertEquals(12, INTEGER.<Integer>valueOf(IntNode.valueOf(12)));
    assertEquals(123L, LONG.<Long>valueOf(LongNode.valueOf(123L)));
    assertEquals("Test", STRING.valueOf(TextNode.valueOf("Test")));
    assertThrows(IllegalArgumentException.class, () -> OBJECT.valueOf(TextNode.valueOf("Test")));
  }
}
