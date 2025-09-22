package org.opentripplanner.graph_builder.module;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.geometry.Coordinates.KONGSBERG_PLATFORM_1;
import static org.opentripplanner.routing.linking.VisibilityMode.TRAVERSE_AREA_EDGES;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

class StreetLinkerModuleTest {

  private static final double DELTA = 0.0001;

  @Test
  void linkingIsIdempotent() {
    var model = new TestModel();
    var module = model.streetLinkerModule();

    module.buildGraph();
    module.buildGraph();
    module.buildGraph();

    assertTrue(model.stopVertex().isConnectedToGraph());
    assertThat(model.stopVertex().getOutgoing()).hasSize(1);
  }

  @Test
  void linkRegularStop() {
    var model = new TestModel();
    var module = model.streetLinkerModule();

    module.buildGraph();

    assertTrue(model.stopVertex().isConnectedToGraph());

    assertThat(model.stopVertex().getOutgoing()).hasSize(1);
    var outgoing = model.outgoingLinks().getFirst();
    assertInstanceOf(StreetTransitStopLink.class, outgoing);

    SplitterVertex linkedTo = (SplitterVertex) outgoing.getToVertex();

    assertTrue(linkedTo.isConnectedToWalkingEdge());
    assertFalse(linkedTo.isConnectedToDriveableEdge());
  }

  @Test
  void linkFlexStop() {
    OTPFeature.FlexRouting.testOn(() -> {
      var model = new TestModel();
      var flexTrip = TimetableRepositoryForTest.of()
        .unscheduledTrip("flex", model.stop(), model.stop());
      model.withFlexTrip(flexTrip);

      var module = model.streetLinkerModule();

      module.buildGraph();

      assertTrue(model.stopVertex().isConnectedToGraph());

      // stop is used by a flex trip, needs to be linked to both the walk and car edge
      assertThat(model.stopVertex().getOutgoing()).hasSize(2);
      var linkToWalk = model.outgoingLinks().getFirst();
      SplitterVertex walkSplit = (SplitterVertex) linkToWalk.getToVertex();

      assertTrue(walkSplit.isConnectedToWalkingEdge());
      assertFalse(walkSplit.isConnectedToDriveableEdge());

      var linkToCar = model.outgoingLinks().getLast();
      SplitterVertex carSplit = (SplitterVertex) linkToCar.getToVertex();

      assertFalse(carSplit.isConnectedToWalkingEdge());
      assertTrue(carSplit.isConnectedToDriveableEdge());
    });
  }

  @Test
  void linkFlexStopWithBoardingLocation() {
    OTPFeature.FlexRouting.testOn(() -> {
      var model = new TestModel().withStopLinkedToBoardingLocation();
      var flexTrip = TimetableRepositoryForTest.of()
        .unscheduledTrip("flex", model.stop(), model.stop());
      model.withFlexTrip(flexTrip);

      var module = model.streetLinkerModule();

      module.buildGraph();

      assertTrue(model.stopVertex().isConnectedToGraph());

      // stop is used by a flex trip, needs to be linked to both the walk and car edge,
      // also linked to the boarding location
      assertThat(model.stopVertex().getOutgoing()).hasSize(3);

      // while the order of the link doesn't matter, it _is_ deterministic.
      // first we have the link to the boarding location where the passengers are expected
      // to wait.
      var links = model.outgoingLinks();
      assertInstanceOf(BoardingLocationToStopLink.class, links.getFirst());

      // the second link is the link to the walkable street network. this is not really necessary
      // because the boarding location is walkable. this will be refactored away in the future.
      var linkToWalk = links.get(1);
      SplitterVertex walkSplit = (SplitterVertex) linkToWalk.getToVertex();

      assertTrue(walkSplit.isConnectedToWalkingEdge());
      assertFalse(walkSplit.isConnectedToDriveableEdge());

      // lastly we have the link to the drivable street network because vehicles also need to
      // reach the stop if it's part of a flex trip.
      var linkToCar = links.getLast();
      SplitterVertex carSplit = (SplitterVertex) linkToCar.getToVertex();

      assertFalse(carSplit.isConnectedToWalkingEdge());
      assertTrue(carSplit.isConnectedToDriveableEdge());
    });
  }

  @Test
  void linkCarsAllowedStop() {
    var model = new TestModel();
    var carsAllowedTrip = TimetableRepositoryForTest.of()
      .trip("carsAllowedTrip")
      .withCarsAllowed(CarAccess.ALLOWED)
      .build();
    model.withCarsAllowedTrip(carsAllowedTrip, model.stop());

    var module = model.streetLinkerModule();

    module.buildGraph();

    assertTrue(model.stopVertex().isConnectedToGraph());

    // Because the stop is used by a carsAllowed trip it needs to be linked to both the walk and car edge
    assertThat(model.stopVertex().getOutgoing()).hasSize(2);
    var linkToWalk = model.outgoingLinks().getFirst();
    SplitterVertex walkSplit = (SplitterVertex) linkToWalk.getToVertex();

    assertTrue(walkSplit.isConnectedToWalkingEdge());
    assertFalse(walkSplit.isConnectedToDriveableEdge());

    var linkToCar = model.outgoingLinks().getLast();
    SplitterVertex carSplit = (SplitterVertex) linkToCar.getToVertex();

    assertFalse(carSplit.isConnectedToWalkingEdge());
    assertTrue(carSplit.isConnectedToDriveableEdge());
  }

  private static class TestModel {

    private final TransitStopVertex stopVertex;
    private final StreetLinkerModule module;
    private final RegularStop stop;
    private final TimetableRepository timetableRepository;
    private final Graph graph;

    public TestModel() {
      var from = StreetModelForTest.intersectionVertex(
        KONGSBERG_PLATFORM_1.y - DELTA,
        KONGSBERG_PLATFORM_1.x - DELTA
      );
      var to = StreetModelForTest.intersectionVertex(
        KONGSBERG_PLATFORM_1.y + DELTA,
        KONGSBERG_PLATFORM_1.x + DELTA
      );

      this.graph = new Graph();
      graph.addVertex(from);
      graph.addVertex(to);

      var walkableEdge = StreetModelForTest.streetEdge(from, to, PEDESTRIAN);
      var drivableEdge = StreetModelForTest.streetEdge(from, to, CAR);
      var builder = SiteRepository.of();
      stop = builder
        .regularStop(id("platform-1"))
        .withCoordinate(new WgsCoordinate(KONGSBERG_PLATFORM_1))
        .build();
      builder.withRegularStop(stop);

      timetableRepository = new TimetableRepository(builder.build(), new Deduplicator());

      stopVertex = TransitStopVertex.of().withStop(stop).build();
      graph.addVertex(stopVertex);
      graph.hasStreets = true;

      module = new StreetLinkerModule(
        graph,
        new VertexLinker(graph, TRAVERSE_AREA_EDGES, 0),
        new DefaultVehicleParkingRepository(),
        timetableRepository,
        DataImportIssueStore.NOOP
      );

      assertFalse(stopVertex.isConnectedToGraph());
      assertTrue(stopVertex.getIncoming().isEmpty());
      assertTrue(stopVertex.getOutgoing().isEmpty());
    }

    public TransitStopVertex stopVertex() {
      return stopVertex;
    }

    public StreetLinkerModule streetLinkerModule() {
      return module;
    }

    public List<Edge> outgoingLinks() {
      return List.copyOf(stopVertex.getOutgoing());
    }

    public RegularStop stop() {
      return stop;
    }

    public void withFlexTrip(UnscheduledTrip flexTrip) {
      timetableRepository.addFlexTrip(flexTrip.getId(), flexTrip);
    }

    public void withCarsAllowedTrip(Trip trip, StopLocation... stops) {
      Route route = TimetableRepositoryForTest.route("carsAllowedRoute").build();
      var stopTimes = Arrays.stream(stops)
        .map(s -> {
          var stopTime = new StopTime();
          stopTime.setStop(s);
          stopTime.setArrivalTime(30);
          stopTime.setDepartureTime(60);
          stopTime.setTrip(trip);
          return stopTime;
        })
        .toList();
      StopPattern stopPattern = new StopPattern(stopTimes);
      var tripTimes = TripTimesFactory.tripTimes(
        trip,
        stopTimes,
        timetableRepository.getDeduplicator()
      );
      TripPattern tripPattern = TimetableRepositoryForTest.tripPattern(
        "carsAllowedTripPattern",
        route
      )
        .withStopPattern(stopPattern)
        .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
        .build();

      timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
    }

    /**
     * Links the stop to a boarding location as can happen during regular graph build.
     */
    public TestModel withStopLinkedToBoardingLocation() {
      var boardingLocation = new OsmBoardingLocationVertex(
        "boarding-location",
        KONGSBERG_PLATFORM_1.x - 0.0001,
        KONGSBERG_PLATFORM_1.y - 0.0001,
        null,
        Set.of(stop.getId().getId())
      );
      graph.addVertex(boardingLocation);

      BoardingLocationToStopLink.createBoardingLocationToStopLink(boardingLocation, stopVertex);
      BoardingLocationToStopLink.createBoardingLocationToStopLink(stopVertex, boardingLocation);
      return this;
    }
  }
}
