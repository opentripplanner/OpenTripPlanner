package org.opentripplanner.modularization.harness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlobTest {

  @Test
  void starMatchesAnythingIncludingDotsAndBrackets() {
    var glob = new Glob("data.stops[*].name");
    assertTrue(glob.matches("data.stops[12].name"));
    assertFalse(glob.matches("data.stops[12].code"));
    assertTrue(new Glob("*").matches("any.path[3]"));
  }

  @Test
  void regexCharactersAreLiteral() {
    var glob = new Glob("a.b[0]");
    assertTrue(glob.matches("a.b[0]"));
    assertFalse(glob.matches("aXb0"));
  }
}
