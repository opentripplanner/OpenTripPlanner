package org.opentripplanner.routing.api.request.request.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TransitGroupSelectTest {

  @Test
  void testToString() {
    assertEquals("EMPTY", TransitGroupSelect.of().build().toString());
    assertEquals(
      "(modes: [BUS], subModeRegexp: [local.*], agencyIds: [A:1])",
      TransitGroupSelect.of()
        .addModes(List.of(TransitMode.BUS))
        .addSubModeRegexp(List.of("local.*"))
        .addAgencyIds(List.of(new FeedScopedId("A", "1")))
        .build()
        .toString()
    );
  }
}
