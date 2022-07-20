package org.opentripplanner.ext.fares.impl;

import static graphql.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.fares.FareService;

/**
 * @author laurent
 */
public class MultipleFareServiceTest {

  @Test
  public void testAddingMultipleFareService() {
    Fare fare1 = new Fare();
    fare1.addFare(FareType.regular, Currency.getInstance("EUR"), 100);
    FareService fs1 = new SimpleFareService(fare1);

    Fare fare2 = new Fare();
    fare2.addFare(FareType.regular, Currency.getInstance("EUR"), 140);
    fare2.addFare(FareType.student, Currency.getInstance("EUR"), 120);
    FareService fs2 = new SimpleFareService(fare2);

    /*
     * Note: this fare is not very representative, as you should probably always compute a
     * "regular" fare in case you want to add bike and transit fares.
     */
    Fare fare3 = new Fare();
    fare3.addFare(FareType.student, Currency.getInstance("EUR"), 80);
    FareService fs3 = new SimpleFareService(fare3);

    AddingMultipleFareService mfs = new AddingMultipleFareService(new ArrayList<>());
    Fare fare = mfs.getCost(null);
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

  private record SimpleFareService(Fare fare) implements FareService {
    @Override
    public Fare getCost(Itinerary itin) {
      return fare;
    }
  }
}
