package org.opentripplanner.framework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WordListTest {

  @Test
  void addInt() {
    assertEquals("1, 2 and 3", WordList.of().add(1).add(2).add(3).toString());
  }

  @Test
  void testToString() {
    assertEquals("", WordList.of().toString());
    assertEquals("one", WordList.of("one").toString());
    assertEquals("one and two", WordList.of("one", "two").toString());
    assertEquals("one, two and three", WordList.of("one", "two", "three").toString());
    assertEquals("one, two, three and four", WordList.of("one", "two", "three", "four").toString());
  }

  @Test
  void isEmpty() {
    assertTrue(WordList.of().isEmpty());
    assertFalse(WordList.of("one").isEmpty());
  }
}
