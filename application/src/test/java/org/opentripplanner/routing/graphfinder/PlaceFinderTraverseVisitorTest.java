package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_05;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_10;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.tripPattern;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPatternBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

public class PlaceFinderTraverseVisitorTest {

  static TimetableRepositoryForTest model = TimetableRepositoryForTest.of();
  static final Station STATION1 = Station.of(id("S1"))
    .withName(new NonLocalizedString("Station 1"))
    .withCoordinate(1.1, 1.1)
    .build();

  static final Station STATION2 = Station.of(id("S2"))
    .withName(new NonLocalizedString("Station 2"))
    .withCoordinate(1.1, 1.1)
    .build();
  static final RegularStop STOP1 = model
    .stop("stop-1")
    .withCoordinate(new WgsCoordinate(1, 1))
    .withParentStation(STATION1)
    .build();
  static final RegularStop STOP2 = model
    .stop("stop-2")
    .withCoordinate(1.001, 1.001)
    .withParentStation(STATION2)
    .build();

  static final RegularStop STOP3 = model.stop("stop-3").withCoordinate(1.002, 1.002).build();
  static final RegularStop STOP4 = model.stop("stop-4").withCoordinate(1.003, 1.003).build();

  static final Route r = route("r").build();

  static TimetableRepository a = new TimetableRepository();

  static {
    TripPatternBuilder t = tripPattern("trip", r);
    var st1 = new StopTime();
    st1.setStop(STOP1);
    st1.setArrivalTime(T11_00);

    var st2 = new StopTime();
    st2.setStop(STOP2);
    st2.setArrivalTime(T11_05);
    t.withStopPattern(new StopPattern(List.of(st1, st2)));
    a.addTripPattern(id("tp1"), t.build());

    var st3 = new StopTime();
    st3.setStop(STOP3);
    st3.setArrivalTime(T11_10);
    t.withStopPattern(new StopPattern(List.of(st3)));
    a.addTripPattern(id("tp2"), t.build());

    var st4 = new StopTime();
    st4.setStop(STOP4);
    st4.setArrivalTime(T11_10);
    t.withStopPattern(new StopPattern(List.of(st4)));
    a.addTripPattern(id("tp3"), t.build());

    a.index();
  }

  static DefaultTransitService transitService = new DefaultTransitService(a);

  @Test
  void stopsOnly() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      List.of(TransitMode.BUS),
      List.of(PlaceType.STOP),
      null,
      null,
      null,
      null,
      null,
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP2).build();
    visitor.visitVertex(state2);
    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(STOP1, STOP2), res);

    visitor.visitVertex(state1);
  }

  @Test
  void stationsOnly() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      List.of(TransitMode.BUS),
      List.of(PlaceType.STATION),
      null,
      null,
      null,
      null,
      null,
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP2).build();
    visitor.visitVertex(state2);
    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(STATION1, STATION2), res);

    visitor.visitVertex(state1);
  }

  @Test
  void stopsAndStations() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      List.of(TransitMode.BUS),
      List.of(PlaceType.STOP, PlaceType.STATION),
      null,
      null,
      null,
      null,
      null,
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP3).build();
    visitor.visitVertex(state2);

    // Revisited stop should not be added to found places
    visitor.visitVertex(state1);
    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(STATION1, STOP3), res);

    visitor.visitVertex(state1);
  }

  @Test
  void stopsAndStationsWithStationFilter() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      List.of(TransitMode.BUS),
      List.of(PlaceType.STOP, PlaceType.STATION),
      List.of(STOP2.getId(), STOP3.getId()),
      List.of(STATION1.getId()),
      null,
      null,
      null,
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP2).build();
    visitor.visitVertex(state2);

    var state3 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP3).build();
    visitor.visitVertex(state3);

    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    // Stop 3 should be included as it is not part of a station.
    // Stop 2 should not be included as its parent station is not included in the station filter.
    assertEquals(List.of(STATION1, STOP3), res);

    visitor.visitVertex(state1);
  }

  @Test
  void stopsAndStationsWithStopFilter() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      List.of(TransitMode.BUS),
      List.of(PlaceType.STOP, PlaceType.STATION),
      List.of(STOP2.getId()),
      null,
      null,
      null,
      null,
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP2).build();
    visitor.visitVertex(state2);

    var state3 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP3).build();
    visitor.visitVertex(state3);

    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    // Stop 3 should not be included as it is included in the stop filter
    assertEquals(List.of(STATION1, STATION2), res);

    visitor.visitVertex(state1);
  }

  @Test
  void stopsAndStationsWithStopAndStationFilter() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      List.of(TransitMode.BUS),
      List.of(PlaceType.STOP, PlaceType.STATION),
      List.of(STOP4.getId()),
      List.of(STATION1.getId()),
      null,
      null,
      null,
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP2).build();
    visitor.visitVertex(state2);

    var state3 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP3).build();
    visitor.visitVertex(state3);

    var state4 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP4).build();
    visitor.visitVertex(state4);

    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(STATION1, STOP4), res);

    visitor.visitVertex(state1);
  }

  @Test
  void rentalStation() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      null,
      List.of(PlaceType.VEHICLE_RENT),
      null,
      null,
      null,
      null,
      null,
      1,
      500
    );
    var station = new TestVehicleRentalStationBuilder().build();
    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().rentalStation(station).build();
    visitor.visitVertex(state1);

    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(station), res);
  }

  @Test
  void rentalStationWithNetworksFilter() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      null,
      List.of(PlaceType.VEHICLE_RENT),
      null,
      null,
      null,
      null,
      List.of("Network-1"),
      1,
      500
    );
    var station = new TestVehicleRentalStationBuilder().build();
    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().rentalStation(station).build();
    visitor.visitVertex(state1);

    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(station), res);

    visitor = new PlaceFinderTraverseVisitor(
      transitService,
      null,
      List.of(PlaceType.VEHICLE_RENT),
      null,
      null,
      null,
      null,
      List.of("Network-2"),
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    state1 = TestStateBuilder.ofWalking().rentalStation(station).build();
    visitor.visitVertex(state1);

    res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(), res);
  }
}
