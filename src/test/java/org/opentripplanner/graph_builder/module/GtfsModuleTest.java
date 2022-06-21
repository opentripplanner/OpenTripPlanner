package org.opentripplanner.graph_builder.module;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;

class GtfsModuleTest {

  @Test
  public void addShapesForFrequencyTrips() {
    var graph = new Graph();

    var bundle = new GtfsBundle(new File(ConstantsForTests.FAKE_GTFS));
    var module = new GtfsModule(List.of(bundle), ServiceDateInterval.unbounded(), null, false);

    module.buildGraph(graph, new HashMap<>());

    var frequencyTripPattern = graph
      .getTripPatterns()
      .stream()
      .filter(p -> !p.getScheduledTimetable().getFrequencyEntries().isEmpty())
      .toList();

    assertEquals(1, frequencyTripPattern.size());

    var tripPattern = frequencyTripPattern.get(0);
    assertNotNull(tripPattern.getGeometry());
    assertNotNull(tripPattern.getHopGeometry(0));

    var pattern = graph.getTripPatternForId(tripPattern.getId());
    assertNotNull(pattern.getGeometry());
    assertNotNull(pattern.getHopGeometry(0));
  }
}
