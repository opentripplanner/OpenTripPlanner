package org.opentripplanner.gtfs.graphbuilder;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

class GtfsModuleTest {

  @Test
  void addShapesForFrequencyTrips() {
    var model = buildTestModel();

    var bundle = GtfsBundle.forTest(ConstantsForTests.SIMPLE_GTFS);
    var module = GtfsModule.forTest(
      List.of(bundle),
      model.timetableRepository,
      model.graph,
      ServiceDateInterval.unbounded()
    );

    module.buildGraph();

    var frequencyTripPattern = model.timetableRepository
      .getAllTripPatterns()
      .stream()
      .filter(p -> !p.getScheduledTimetable().getFrequencyEntries().isEmpty())
      .toList();

    assertEquals(1, frequencyTripPattern.size());

    var tripPattern = frequencyTripPattern.getFirst();
    assertNotNull(tripPattern.getGeometry());
    assertNotNull(tripPattern.getHopGeometry(0));

    var pattern = model.timetableRepository.getTripPatternForId(tripPattern.getId());
    assertNotNull(pattern.getGeometry());
    assertNotNull(pattern.getHopGeometry(0));
  }

  @Test
  void duplicateFeedId() {
    var bundles = List.of(bundle("A"), bundle("A"));
    var model = buildTestModel();

    var module = GtfsModule.forTest(
      bundles,
      model.timetableRepository,
      model.graph,
      ServiceDateInterval.unbounded()
    );
    assertThrows(IllegalArgumentException.class, module::buildGraph);
  }

  private static TestModels buildTestModel() {
    var deduplicator = new Deduplicator();
    var siteRepository = new SiteRepository();
    var graph = new Graph(deduplicator);
    var timetableRepository = new TimetableRepository(siteRepository, deduplicator);
    return new TestModels(graph, timetableRepository);
  }

  record TestModels(Graph graph, TimetableRepository timetableRepository) {}

  static GtfsBundle bundle(String feedId) {
    return GtfsBundle.forTest(
      ResourceLoader.of(GtfsModuleTest.class).file("/gtfs/interlining"),
      feedId
    );
  }

  @Nested
  class Interlining {

    static List<Arguments> interliningCases() {
      return List.of(
        Arguments.of(List.of(bundle("A")), 2),
        Arguments.of(List.of(bundle("A"), bundle("B")), 4),
        Arguments.of(List.of(bundle("A"), bundle("B"), bundle("C")), 6)
      );
    }

    /**
     * We test that the number of stay-seated transfers grows linearly (not exponentially) with the
     * number of GTFS input feeds.
     */
    @ParameterizedTest(name = "Bundles {0} should generate {1} stay-seated transfers")
    @MethodSource("interliningCases")
    void interline(List<GtfsBundle> bundles, int expectedTransfers) {
      var model = buildTestModel();

      var feedIds = bundles.stream().map(GtfsBundle::getFeedId).collect(Collectors.toSet());
      assertEquals(bundles.size(), feedIds.size());

      var module = GtfsModule.forTest(
        bundles,
        model.timetableRepository,
        model.graph,
        ServiceDateInterval.unbounded()
      );

      module.buildGraph();

      assertEquals(
        expectedTransfers,
        model.timetableRepository.getTransferService().listAll().size()
      );
    }
  }
}
