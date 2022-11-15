package org.opentripplanner.ext.realtimeresolver;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

class RealtimeResolverTest {

  private static MockLeg MOCK_LEG = new MockLeg();

  private static class MockLeg implements Leg {

    @Override
    public boolean isTransitLeg() {
      return true;
    }

    @Override
    public boolean hasSameMode(Leg other) {
      return false;
    }

    @Override
    public ZonedDateTime getStartTime() {
      return ZonedDateTime.now();
    }

    @Override
    public ZonedDateTime getEndTime() {
      return ZonedDateTime.now();
    }

    @Override
    public double getDistanceMeters() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public Place getFrom() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public Place getTo() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public LineString getLegGeometry() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public int getGeneralizedCost() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public LegReference getLegReference() {
      return new MockLegReference();
    }
  }

  private static class MockLegReference implements LegReference {

    @Override
    public Leg getLeg(TransitService transitService) {
      return MOCK_LEG;
    }
  }

  @Test
  void testPopulateLegsWithRealtime() {
    var legs = new ArrayList<Leg>();
    legs.add(new MockLeg());
    legs.add(new MockLeg());
    legs.add(new MockLeg());

    var itineraries = new ArrayList<Itinerary>();
    itineraries.add(new Itinerary(legs));
    itineraries.add(new Itinerary(legs));
    itineraries.add(new Itinerary(legs));

    RealtimeResolver.populateLegsWithRealtime(itineraries, new DefaultTransitService(new TransitModel()));

    assertEquals(3, itineraries.size());

    itineraries.forEach(it -> {
      var lgs = it.getLegs();
      assertEquals(3, lgs.size());
      lgs.forEach(l -> assertEquals(MOCK_LEG, l));
    });
  }
}
