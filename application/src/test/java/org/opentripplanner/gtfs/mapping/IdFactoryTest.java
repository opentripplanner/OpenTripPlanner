package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.SiteRepository;

class IdFactoryTest {

  private final IdFactory FACTORY = new IdFactory("B");

  @Test
  void createIdFromAgencyAndId() {
    org.onebusaway.gtfs.model.AgencyAndId inputId = new org.onebusaway.gtfs.model.AgencyAndId(
      "A",
      "1"
    );

    FeedScopedId mappedId = FACTORY.createNullableId(inputId);

    assertEquals("B", mappedId.getFeedId());
    assertEquals("1", mappedId.getId());
  }

  @Test
  void emptyAgencyAndId() {
    assertThrows(IllegalArgumentException.class, () ->
      FACTORY.createNullableId(new org.onebusaway.gtfs.model.AgencyAndId())
    );
  }

  @Test
  void nullAgencyAndId() {
    assertNull(FACTORY.createNullableId((AgencyAndId) null));
  }

  private static Stream<AgencyAndId> invalidCases() {
    return Stream.of(
      null,
      new AgencyAndId("1", null),
      new AgencyAndId("1", ""),
      new AgencyAndId("1", "\t")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidCases")
  void invalidId(AgencyAndId id) {
    var ex = assertThrows(RuntimeException.class, () -> FACTORY.createId(id, "thing"));
    assertEquals(
      "Error during GTFS processing: id of thing is null or consists of whitespace only",
      ex.getMessage()
    );
  }
}
