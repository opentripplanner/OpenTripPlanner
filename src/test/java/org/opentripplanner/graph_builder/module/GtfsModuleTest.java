package org.opentripplanner.graph_builder.module;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class GtfsModuleTest {

  @Test
  public void addShapesForFrequencyTrips() {
    var model = buildTestModel();

    var bundle = new GtfsBundle(new File(ConstantsForTests.FAKE_GTFS));
    var module = new GtfsModule(
      List.of(bundle),
      model.transitModel,
      model.graph,
      ServiceDateInterval.unbounded()
    );

    module.buildGraph();

    var frequencyTripPattern = model.transitModel
      .getAllTripPatterns()
      .stream()
      .filter(p -> !p.getScheduledTimetable().getFrequencyEntries().isEmpty())
      .toList();

    assertEquals(1, frequencyTripPattern.size());

    var tripPattern = frequencyTripPattern.get(0);
    assertNotNull(tripPattern.getGeometry());
    assertNotNull(tripPattern.getHopGeometry(0));

    var pattern = model.transitModel.getTripPatternForId(tripPattern.getId());
    assertNotNull(pattern.getGeometry());
    assertNotNull(pattern.getHopGeometry(0));
  }

  private static TestModels buildTestModel() {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);
    return new TestModels(graph, transitModel);
  }

  record TestModels(Graph graph, TransitModel transitModel) {}

  @Nested
  class Interlining {

    static GtfsBundle bundle(String feedId) {
      var b = new GtfsBundle(new File("src/test/resources/gtfs/interlining"));
      b.setFeedId(new GtfsFeedId.Builder().id(feedId).build());
      return b;
    }

    static Stream<Arguments> interliningCases = Stream.of(
      Arguments.of(List.of(bundle("A")), 2),
      Arguments.of(List.of(bundle("A"), bundle("B")), 4),
      Arguments.of(List.of(bundle("A"), bundle("B"), bundle("C")), 6)
    );

    /**
     * We test that the number of stay-seated transfers grows linearly (not exponentially) with the
     * number of GTFS input feeds.
     */
    @ParameterizedTest(name = "Bundles {0} should generate {1} stay-seated transfers")
    @VariableSource("interliningCases")
    public void interline(List<GtfsBundle> bundles, int expectedTransfers) {
      var model = buildTestModel();

      var feedIds = bundles.stream().map(GtfsBundle::getFeedId).collect(Collectors.toSet());
      assertEquals(bundles.size(), feedIds.size());

      var module = new GtfsModule(
        bundles,
        model.transitModel,
        model.graph,
        ServiceDateInterval.unbounded()
      );

      module.buildGraph();

      assertEquals(expectedTransfers, model.transitModel.getTransferService().listAll().size());
    }
  }
}
