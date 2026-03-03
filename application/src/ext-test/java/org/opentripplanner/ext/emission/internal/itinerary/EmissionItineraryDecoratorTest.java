package org.opentripplanner.ext.emission.internal.itinerary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.emission.EmissionService;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionService;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;

class EmissionItineraryDecoratorTest implements PlanTestConstants {

  // Override PlanTestConstants places with stops that are close together so geometry-based
  // transit distances produce reasonable emission values.
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private static final Place A = Place.forStop(
    ENV_BUILDER.stop("A", b -> b.withCoordinate(60.0, 10.000))
  );
  private static final Place B = Place.forStop(
    ENV_BUILDER.stop("B", b -> b.withCoordinate(60.0, 10.012))
  );
  private static final Place C = Place.forStop(
    ENV_BUILDER.stop("C", b -> b.withCoordinate(60.0, 10.020))
  );
  private static final Place D = Place.forStop(
    ENV_BUILDER.stop("D", b -> b.withCoordinate(60.0, 10.030))
  );
  private static final Place E = Place.forStop(
    ENV_BUILDER.stop("E", b -> b.withCoordinate(60.0, 10.040))
  );

  private static final int START_TIME = T11_09;
  private static final int END_TIME = T11_55;

  // Apply route emissions
  private Itinerary bus;
  // Apply route emissions with zero emissions
  private Itinerary busZeroEmission;
  // Apply trip hop emissions
  private Itinerary rail;
  // Car emissions
  private Itinerary car;
  // No emissions
  private Itinerary flex;

  private Itinerary combinedWithFlex;
  private Itinerary combinedNoFlex;

  private EmissionService emissionService;
  private Emission expectedRailEmission = Emission.ZERO;

  @BeforeEach
  void setUpItineraries() {
    var routeA = ENV_BUILDER.route("R1");
    var routeB = ENV_BUILDER.route("R2");

    bus = newItinerary(A).bus(routeA, 21, START_TIME, END_TIME, B).build();
    busZeroEmission = newItinerary(A).bus(routeB, 22, START_TIME, END_TIME, B).build();
    rail = newItinerary(A).rail(3, START_TIME, END_TIME, B).build();
    car = newItinerary(A).drive(START_TIME, END_TIME, B).build();
    flex = newItinerary(A).flex(START_TIME, END_TIME, B).build();

    int t0 = START_TIME;
    combinedWithFlex = newItinerary(A)
      .drive(t0, t0 += 90, C)
      .rail(4, t0 += 90, t0 += 90, D)
      .bus(routeA, 20, t0, t0 += 90, E)
      .flex(t0 + 90, END_TIME, B)
      .build();

    combinedNoFlex = newItinerary(A)
      .drive(t0, t0 += 90, C)
      .rail(4, t0 += 90, t0 + 90, D)
      .bus(routeA, 20, t0, t0 += 90, E)
      .bus(routeB, 23, t0, END_TIME, B)
      .build();

    var repository = new DefaultEmissionRepository();

    // Set car emissions
    repository.setCarAvgCo2PerMeter(Gram.of(0.015));

    // Set route emissions for bus - using route emission
    Map<FeedScopedId, Emission> routeEmissions = new HashMap<>();
    routeEmissions.put(routeA.getId(), Emission.ofCo2Gram(0.01));
    routeEmissions.put(routeB.getId(), Emission.ZERO);
    repository.addRouteEmissions(routeEmissions);

    // Set trip emissions for rail - using trip-pattern emission
    Map<FeedScopedId, TripPatternEmission> tripEmissions = new HashMap<>();
    var hopEmissions = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
      .stream()
      .map(Emission::ofCo2Gram)
      .toList();
    var rLeg = rail.legs().getFirst();
    assertTrue(
      hopEmissions.size() >= rLeg.alightStopPosInPattern(),
      "The list of emissions must be long enough, at least >= alightStopPosInPattern."
    );

    for (int i = rLeg.boardStopPosInPattern(); i < rLeg.alightStopPosInPattern(); ++i) {
      expectedRailEmission = expectedRailEmission.plus(hopEmissions.get(i));
    }
    tripEmissions.put(rLeg.trip().getId(), new TripPatternEmission(hopEmissions));
    tripEmissions.put(
      combinedWithFlex.legs().get(1).trip().getId(),
      new TripPatternEmission(hopEmissions)
    );
    tripEmissions.put(
      combinedNoFlex.legs().get(1).trip().getId(),
      new TripPatternEmission(hopEmissions)
    );

    repository.addTripPatternEmissions(tripEmissions);

    emissionService = new DefaultEmissionService(repository);
  }

  @Test
  void decorateBusUsingRouteEmission() {
    var subject = new EmissionItineraryDecorator(emissionService);
    var it = subject.decorate(bus);
    assertEquals(Emission.ofCo2Gram(6.672), it.emissionPerPerson());
    assertEquals(Emission.ofCo2Gram(6.672), it.legs().getFirst().emissionPerPerson());
  }

  @Test
  void decorateBusUsingZeroRouteEmission() {
    var subject = new EmissionItineraryDecorator(emissionService);
    var it = subject.decorate(busZeroEmission);
    assertEquals(Emission.ZERO, it.emissionPerPerson());
    assertEquals(Emission.ZERO, it.legs().getFirst().emissionPerPerson());
  }

  @Test
  void decorateRailUsingTripPatternEmission() {
    var subject = new EmissionItineraryDecorator(emissionService);
    var it = subject.decorate(rail);
    assertEquals(expectedRailEmission, it.emissionPerPerson());
    assertEquals(expectedRailEmission, it.legs().getFirst().emissionPerPerson());
  }

  @Test
  void decorateCar() {
    var subject = new EmissionItineraryDecorator(emissionService);
    var it = subject.decorate(car);
    assertEquals(Emission.ofCo2Gram(1035), it.emissionPerPerson());
    assertEquals(Emission.ofCo2Gram(1035), it.legs().getFirst().emissionPerPerson());
  }

  @Test
  void decorateFlex() {
    var subject = new EmissionItineraryDecorator(emissionService);
    var it = subject.decorate(flex);
    assertNull(it.emissionPerPerson());
    assertNull(it.legs().getFirst().emissionPerPerson());
  }

  @Test
  void decorateCombinedWithFlex() {
    var subject = new EmissionItineraryDecorator(emissionService);

    var it = subject.decorate(combinedWithFlex);

    assertNull(it.emissionPerPerson());
    // car - bus - rail - flex
    assertEmission(33.75, it.legs().get(0).emissionPerPerson());
    assertEmission(31, it.legs().get(1).emissionPerPerson());
    assertEmission(5.56, it.legs().get(2).emissionPerPerson());
    assertNull(it.legs().get(3).emissionPerPerson());
  }

  @Test
  void decorateCombinedNoFlex() {
    var subject = new EmissionItineraryDecorator(emissionService);

    var it = subject.decorate(combinedNoFlex);

    assertEmission(70.31, it.emissionPerPerson());
    // car - rail - bus(routeA) - bus(routeB, zero emission)
    assertEmission(33.75, it.legs().get(0).emissionPerPerson());
    assertEmission(31, it.legs().get(1).emissionPerPerson());
    assertEmission(5.56, it.legs().get(2).emissionPerPerson());
  }

  private void assertEmission(double expectedCo2, Emission actual) {
    assertEquals(Emission.ofCo2Gram(expectedCo2), actual);
  }
}
