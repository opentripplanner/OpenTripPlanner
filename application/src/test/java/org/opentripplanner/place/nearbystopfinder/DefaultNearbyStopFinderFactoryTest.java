package org.opentripplanner.place.nearbystopfinder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.opentripplanner.place.DefaultNearbyStopFinderFactory;
import org.opentripplanner.place.NearbyStopFinderFactory;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;

class DefaultNearbyStopFinderFactoryTest {

  @Test
  void createReturnsStraightLineFinderWhenGraphHasNoStreets() {
    var graph = new Graph();

    assertInstanceOf(StraightLineNearbyStopFinder.class, subject(graph).create());
  }

  @Test
  void createReturnsStreetFinderWhenGraphHasStreets() {
    var graph = new Graph();
    graph.hasStreets = true;

    assertInstanceOf(StreetNearbyStopFinder.class, subject(graph).create());
  }

  private static NearbyStopFinderFactory subject(Graph graph) {
    return new DefaultNearbyStopFinderFactory(
      graph,
      new DefaultTransitService(new TransitRepository()),
      new LinkingContextFactory(graph, new VertexCreationService(VertexLinkerTestFactory.of(graph)))
    );
  }
}