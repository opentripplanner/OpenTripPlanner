package org.opentripplanner.ext.fares.impl;

import static graphql.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.basic.Money;

/**
 * @author laurent
 */
public class MultipleFareServiceTest {

  @Test
  public void testAddingMultipleFareService() {
    ItineraryFares fare1 = new ItineraryFares();
    fare1.addFare(FareType.regular, Money.euros(100));
    FareService fs1 = new SimpleFareService(fare1);

    ItineraryFares fare2 = new ItineraryFares();
    fare2.addFare(FareType.regular, Money.euros(140));
    fare2.addFare(FareType.student, Money.euros(120));
    FareService fs2 = new SimpleFareService(fare2);

    /*
     * Note: this fare is not very representative, as you should probably always compute a
     * "regular" fare in case you want to add bike and transit fares.
     */
    ItineraryFares fare3 = new ItineraryFares();
    fare3.addFare(FareType.student, Money.euros(80));
    FareService fs3 = new SimpleFareService(fare3);

    AddingMultipleFareService mfs = new AddingMultipleFareService(new ArrayList<>());
    ItineraryFares fare = mfs.getCost(null);
    assertNull(fare);

    mfs = new AddingMultipleFareService(List.of(fs1));
    fare = mfs.getCost(null);
    assertEquals(100, fare.getFare(FareType.regular).cents());
    assertNull(fare.getFare(FareType.student));

    mfs = new AddingMultipleFareService(List.of(fs2));
    fare = mfs.getCost(null);
    assertEquals(140, fare.getFare(FareType.regular).cents());
    assertEquals(120, fare.getFare(FareType.student).cents());

    mfs = new AddingMultipleFareService(Arrays.asList(fs1, fs2));
    fare = mfs.getCost(null);
    assertEquals(240, fare.getFare(FareType.regular).cents());
    assertEquals(220, fare.getFare(FareType.student).cents());

    mfs = new AddingMultipleFareService(Arrays.asList(fs2, fs1));
    fare = mfs.getCost(null);
    assertEquals(240, fare.getFare(FareType.regular).cents());
    assertEquals(220, fare.getFare(FareType.student).cents());

    mfs = new AddingMultipleFareService(Arrays.asList(fs1, fs3));
    fare = mfs.getCost(null);
    assertEquals(100, fare.getFare(FareType.regular).cents());
    assertEquals(180, fare.getFare(FareType.student).cents());

    mfs = new AddingMultipleFareService(Arrays.asList(fs3, fs1));
    fare = mfs.getCost(null);
    assertEquals(100, fare.getFare(FareType.regular).cents());
    assertEquals(180, fare.getFare(FareType.student).cents());

    mfs = new AddingMultipleFareService(Arrays.asList(fs1, fs2, fs3));
    fare = mfs.getCost(null);
    assertEquals(240, fare.getFare(FareType.regular).cents());
    assertEquals(300, fare.getFare(FareType.student).cents());
  }

  private record SimpleFareService(ItineraryFares fare) implements FareService {
    @Override
    public ItineraryFares getCost(Itinerary itin) {
      return fare;
    }
  }
}
