package org.opentripplanner.framework.collection;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.collection.MapUtils.mapToList;

import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (30.10.2017)
 */
public class MapUtilsTest {

  @Test
  public void mapToListTest() throws Exception {
    assertNull(mapToList(null, identity()));
    assertTrue(mapToList(Collections.emptyList(), identity()).isEmpty());
    assertEquals(singletonList(5), mapToList(singleton(5), identity()));
  }
}
