package org.opentripplanner.graph_builder.module;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class GtfsModuleTest {

  @Test
  public void addShapesForFrequencyTrips() {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(stopModel, deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);

    var bundle = new GtfsBundle(new File(ConstantsForTests.FAKE_GTFS));
    var module = new GtfsModule(
      List.of(bundle),
      transitModel,
      graph,
      ServiceDateInterval.unbounded()
    );

    module.buildGraph();

    var frequencyTripPattern = transitModel
      .getAllTripPatterns()
      .stream()
      .filter(p -> !p.getScheduledTimetable().getFrequencyEntries().isEmpty())
      .toList();

    assertEquals(1, frequencyTripPattern.size());

    var tripPattern = frequencyTripPattern.get(0);
    assertNotNull(tripPattern.getGeometry());
    assertNotNull(tripPattern.getHopGeometry(0));

    var pattern = transitModel.getTripPatternForId(tripPattern.getId());
    assertNotNull(pattern.getGeometry());
    assertNotNull(pattern.getHopGeometry(0));
  }
}
