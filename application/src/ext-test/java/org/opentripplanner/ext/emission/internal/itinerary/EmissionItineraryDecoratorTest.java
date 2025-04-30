package org.opentripplanner.ext.emission.internal.itinerary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.emission.EmissionService;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionService;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class EmissionItineraryDecoratorTest implements PlanTestConstants {

  Itinerary bus;
  Itinerary car;
  EmissionService emissionService;

  @BeforeEach
  void setUpItineraries() {
    bus = newItinerary(A).bus(21, T11_06, T11_09, B).build();
    car = newItinerary(A).drive(T11_30, T11_50, B).build();
    var repository = new DefaultEmissionRepository();
    Map<FeedScopedId, Emission> emissions = new HashMap<>();
    emissions.put(bus.firstLeg().getRoute().getId(), Emission.co2_g(0.001));
    repository.addRouteEmissions(emissions);
    repository.setCarAvgCo2PerMeter(0.0015);
    emissionService = new DefaultEmissionService(repository);
  }

  @Test
  void decorateBus() {
    var subject = new EmissionItineraryDecorator(emissionService);
    var it = subject.decorate(bus);
    assertEquals(Emission.co2_g(2.25), it.emissionPerPerson());
  }

  @Test
  void decorateCar() {
    var subject = new EmissionItineraryDecorator(emissionService);
    var it = subject.decorate(car);
    assertEquals(Emission.co2_g(45), it.emissionPerPerson());
  }
}
