package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_05;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;
import static org.opentripplanner.transit.model._data.TransitModelForTest.route;
import static org.opentripplanner.transit.model._data.TransitModelForTest.tripPattern;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPatternBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

public class PlaceFinderTraverseVisitorTest {

  static final Station STATION = Station
    .of(id("S"))
    .withName(new NonLocalizedString("Station"))
    .withCoordinate(1.1, 1.1)
    .build();
  static final RegularStop STOP1 = TransitModelForTest.stopForTest("stop-1", 1, 1, STATION);
  static final RegularStop STOP2 = TransitModelForTest.stopForTest("stop-2", 1.001, 1.001);

  static final Route r = route("r").build();

  static TransitModel a = new TransitModel();

  static {
    a.addTransitMode(TransitMode.BUS);
    TripPatternBuilder t = tripPattern("trip", r);
    var st1 = new StopTime();
    st1.setStop(STOP1);
    st1.setArrivalTime(T11_00);

    var st2 = new StopTime();
    st2.setStop(STOP2);
    st2.setArrivalTime(T11_05);
    t.withStopPattern(new StopPattern(List.of(st1, st2)));
    a.addTripPattern(id("asd"), t.build());

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
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP2).build();
    visitor.visitVertex(state2);
    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(STATION), res);

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
      1,
      500
    );

    assertEquals(List.of(), visitor.placesFound);
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP1).build();

    visitor.visitVertex(state1);

    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP2).build();
    visitor.visitVertex(state2);

    // Revisited stop should not be added to found places
    visitor.visitVertex(state1);
    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    assertEquals(List.of(STATION, STOP2), res);

    visitor.visitVertex(state1);
  }

  @Test
  void noStationsByDefault() {
    var visitor = new PlaceFinderTraverseVisitor(
      transitService,
      List.of(TransitMode.BUS),
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

    // Revisited stop should not be added to found places
    visitor.visitVertex(state1);
    var res = visitor.placesFound.stream().map(PlaceAtDistance::place).toList();

    // One trip pattern should also be found on default settings
    var pattern = new PatternAtStop(STOP1, a.getAllTripPatterns().stream().findFirst().get());

    assertEquals(List.of(STOP1, pattern, STOP2), res);

    visitor.visitVertex(state1);
  }
}
