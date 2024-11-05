package org.opentripplanner.utils.collection;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.utils.collection.MapUtils.mapToList;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (30.10.2017)
 */
public class MapUtilsTest {

  @Test
  public void mapToListTest() {
    assertNull(mapToList(null, identity()));
    assertTrue(mapToList(Collections.emptyList(), identity()).isEmpty());
    assertEquals(singletonList(5), mapToList(singleton(5), identity()));
  }

  @Test
  public void combine() {
    var combined = MapUtils.combine(Map.of("key", "value"), Map.of("key2", "value"));
    assertEquals(Map.of("key", "value", "key2", "value"), combined);
  }

  @Test
  public void combineMultiple() {
    var combined = MapUtils.combine(
      Map.of("key", "value", "key3", "another"),
      Map.of("key2", "value")
    );
    assertEquals(Map.of("key", "value", "key2", "value", "key3", "another"), combined);
  }

  @Test
  public void combineLastOneWins() {
    var combined = MapUtils.combine(Map.of("key", "first value"), Map.of("key", "second value"));
    assertEquals(Map.of("key", "second value"), combined);
  }
}
