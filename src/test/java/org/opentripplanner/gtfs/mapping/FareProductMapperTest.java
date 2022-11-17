package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareProduct;
import org.opentripplanner.transit.model.basic.Money;

class FareProductMapperTest {

  @Test
  void map() {
    var gtfs = new FareProduct();
    gtfs.setFareProductId(new AgencyAndId("1", "1"));
    gtfs.setAmount(1);
    gtfs.setName("day pass");
    gtfs.setCurrency("USD");
    gtfs.setDurationAmount(1);
    gtfs.setDurationUnit(5);

    var mapper = new FareProductMapper();
    var internal = mapper.map(gtfs);

    assertEquals(internal.duration(), Duration.ofDays(31));
    assertEquals(internal.amount(), Money.usDollars(100));
    assertEquals(internal.amount().cents(), 100);
  }

  @Test
  void noFractionDigits() {
    var gtfs = new FareProduct();
    gtfs.setFareProductId(new AgencyAndId("1", "1"));
    gtfs.setAmount(100);
    gtfs.setName("day pass");
    gtfs.setCurrency("JPY");

    var mapper = new FareProductMapper();
    var internal = mapper.map(gtfs);

    assertEquals(internal.amount().toString(), "¥100");
    assertEquals(internal.amount().cents(), 100);
  }
}
