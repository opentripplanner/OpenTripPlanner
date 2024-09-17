package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.search.state.TestStateBuilder;

class GraphPathToItineraryMapperTest {

  @Test
  void isSearchWindowAware() {
    var mapper = new GraphPathToItineraryMapper(ZoneIds.UTC, new StreetNotesService(), 1);
    var state = TestStateBuilder.ofWalking().streetEdge().streetEdge().streetEdge().build();
    var itin = mapper.generateItinerary(new GraphPath<>(state));
    assertFalse(itin.isSearchWindowAware());
  }
}
