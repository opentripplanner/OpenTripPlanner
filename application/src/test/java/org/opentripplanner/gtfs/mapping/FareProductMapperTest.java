package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareProduct;
import org.opentripplanner.transit.model.basic.Money;

class FareProductMapperTest {

  public static final IdFactory ID_FACTORY = new IdFactory("1");

  @Test
  void map() {
    var gtfs = new FareProduct();
    gtfs.setFareProductId(new AgencyAndId("1", "1"));
    gtfs.setAmount(1);
    gtfs.setName("day pass");
    gtfs.setCurrency("USD");
    gtfs.setDurationAmount(1);
    gtfs.setDurationUnit(5);

    var mapper = new FareProductMapper(ID_FACTORY);
    var internal = mapper.map(gtfs);

    assertEquals(internal.price(), Money.usDollars(1));
    assertEquals(internal.price().minorUnitAmount(), 100);
  }

  @Test
  void noFractionDigits() {
    var gtfs = new FareProduct();
    gtfs.setFareProductId(new AgencyAndId("1", "1"));
    gtfs.setAmount(100);
    gtfs.setName("day pass");
    gtfs.setCurrency("JPY");

    var mapper = new FareProductMapper(ID_FACTORY);
    var internal = mapper.map(gtfs);

    assertEquals(internal.price().toString(), "¥100");
    assertEquals(internal.price().minorUnitAmount(), 100);
  }
}
