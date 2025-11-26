package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareProduct;
import org.onebusaway.gtfs.model.RiderCategory;
import org.opentripplanner.transit.model.basic.Money;

class FareProductMapperTest {

  public static final IdFactory ID_FACTORY = new IdFactory("1");
  public static final String CATEGORY_NAME = "Category 1";

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
    assertEquals(100, internal.price().minorUnitAmount());
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

    assertEquals("¥100", internal.price().toString());
    assertEquals(100, internal.price().minorUnitAmount());
  }

  @Test
  void riderCategory() {
    var gtfs = fareProduct();
    gtfs.setRiderCategory(category());

    var mapper = new FareProductMapper(ID_FACTORY);
    var mapped = mapper.map(gtfs).category();

    assertEquals(CATEGORY_NAME, mapped.name());
    assertEquals("1:cat1", mapped.id().toString());
    assertFalse(mapped.isDefault());
  }

  @Test
  void defaultCategory() {
    var gtfs = fareProduct();
    var category = category();
    category.setIsDefaultFareCategory(1);
    gtfs.setRiderCategory(category);

    var mapper = new FareProductMapper(ID_FACTORY);

    var mapped = mapper.map(gtfs).category();

    assertTrue(mapped.isDefault());
  }

  private static RiderCategory category() {
    var category = new RiderCategory();
    category.setId(new AgencyAndId("1", "cat1"));
    category.setName(CATEGORY_NAME);
    return category;
  }

  private static FareProduct fareProduct() {
    var gtfs = new FareProduct();
    gtfs.setFareProductId(new AgencyAndId("1", "1"));
    gtfs.setAmount(1);
    gtfs.setName("day pass");
    gtfs.setCurrency("USD");
    return gtfs;
  }
}
