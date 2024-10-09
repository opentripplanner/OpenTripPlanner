package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.LIMIT_TO_NUM_OF_ITINERARIES;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.LIMIT_TO_SEARCH_WINDOW;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.LIST_ALL;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.OFF;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.doc.DocumentedEnumTestHelper;

class ItineraryFilterDebugProfileTest {

  @Test
  void debugEnabled() {
    assertFalse(OFF.debugEnabled());
    assertTrue(LIST_ALL.debugEnabled());
    assertTrue(LIMIT_TO_SEARCH_WINDOW.debugEnabled());
    assertTrue(LIMIT_TO_NUM_OF_ITINERARIES.debugEnabled());
  }

  @Test
  void fromEnabled() {
    assertEquals(LIST_ALL, ItineraryFilterDebugProfile.ofDebugEnabled(true));
    assertEquals(OFF, ItineraryFilterDebugProfile.ofDebugEnabled(false));
  }

  @Test
  void doc() {
    DocumentedEnumTestHelper.verifyHasDocumentation(ItineraryFilterDebugProfile.values());
  }
}
