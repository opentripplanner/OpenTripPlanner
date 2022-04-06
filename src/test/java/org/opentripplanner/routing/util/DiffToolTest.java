package org.opentripplanner.routing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DiffToolTest {

  @Test
  void diffTwoEmptySet() {
    var d = DiffTool.diff(List.of(), List.of(), String::compareTo);
    assertTrue(d.isEqual());
    assertTrue(d.isEmpty());
  }

  @Test
  void diffLeftIsEmpty() {
    var d = DiffTool.diff(List.of(), List.of("Any"), String::compareTo);
    assertFalse(d.isEqual());
    assertEquals("[(right: Any)]", d.toString());
  }

  @Test
  void diffRightIsEmpty() {
    var d = DiffTool.diff(List.of("Any"), List.of(), String::compareTo);
    assertFalse(d.isEqual());
    assertEquals("[(left: Any)]", d.toString());
  }

  @Test
  void diffTwoEqualSets() {
    var d = DiffTool.diff(List.of("7", "2"), List.of("2", "7"), String::compareTo);
    assertTrue(d.isEqual());
    assertEquals("[(eq: 2), (eq: 7)]", d.toString());
  }

  @Test
  void diffTwoSetWithTwoValuesEachOneMatch() {
    var d = DiffTool.diff(List.of("1", "2"), List.of("2", "3"), String::compareTo);
    assertFalse(d.isEqual());
    assertEquals("[(left: 1), (eq: 2), (right: 3)]", d.toString());
  }
}
