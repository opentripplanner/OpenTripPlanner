package org.opentripplanner.gtfs.mapping;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.opentripplanner.transit.model.basic.Money;

public class FareAttributeMapperTest {

  private static final org.onebusaway.gtfs.model.FareAttribute FARE_ATTRIBUTE =
    new org.onebusaway.gtfs.model.FareAttribute();

  private static final AgencyAndId ID = new AgencyAndId("A", "1");

  private static final String CURRENCY_TYPE = "NOK";

  private static final int JOURNEY_DURATION = 10;

  private static final int PAY_MENTMETHOD = 1;

  private static final float PRICE = 0.3f;

  private static final float SENIOR_PRICE = 0.7f;

  private static final float YOUTH_PRICE = 0.9f;

  private static final int TRANSFER_DURATION = 3;

  private static final int TRANSFERS = 2;
  private static final Currency NOK = Currency.getInstance("NOK");
  private final FareAttributeMapper subject = new FareAttributeMapper();

  static {
    FARE_ATTRIBUTE.setId(ID);
    FARE_ATTRIBUTE.setCurrencyType(CURRENCY_TYPE);
    FARE_ATTRIBUTE.setJourneyDuration(JOURNEY_DURATION);
    FARE_ATTRIBUTE.setPaymentMethod(PAY_MENTMETHOD);
    FARE_ATTRIBUTE.setPrice(PRICE);
    FARE_ATTRIBUTE.setSeniorPrice(SENIOR_PRICE);
    FARE_ATTRIBUTE.setYouthPrice(YOUTH_PRICE);
    FARE_ATTRIBUTE.setTransferDuration(TRANSFER_DURATION);
    FARE_ATTRIBUTE.setTransfers(TRANSFERS);
  }

  @Test
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<FareAttribute>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(singleton(FARE_ATTRIBUTE)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.ext.fares.model.FareAttribute result = subject.map(FARE_ATTRIBUTE);

    assertEquals("A:1", result.getId().toString());
    assertEquals(JOURNEY_DURATION, result.getJourneyDuration());
    assertEquals(PAY_MENTMETHOD, result.getPaymentMethod());
    assertEquals(Money.ofFractionalAmount(NOK, PRICE), result.getPrice());
    assertEquals(TRANSFER_DURATION, result.getTransferDuration());
    assertEquals(TRANSFERS, result.getTransfers());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    FareAttribute orginal = new FareAttribute();
    orginal.setId(ID);
    orginal.setCurrencyType("EUR");
    org.opentripplanner.ext.fares.model.FareAttribute result = subject.map(orginal);

    assertNotNull(result.getId());
    assertFalse(result.isJourneyDurationSet());
    assertEquals(0, result.getPaymentMethod());
    assertEquals(Money.euros(0), result.getPrice());
    assertFalse(result.isTransferDurationSet());
    assertFalse(result.isTransfersSet());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.ext.fares.model.FareAttribute result1 = subject.map(FARE_ATTRIBUTE);
    org.opentripplanner.ext.fares.model.FareAttribute result2 = subject.map(FARE_ATTRIBUTE);

    assertSame(result1, result2);
  }
}
