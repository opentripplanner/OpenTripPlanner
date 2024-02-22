package org.opentripplanner.openstreetmap.tagmapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class HamburgMapperTest {

  private HamburgMapper mapper;

  @BeforeEach
  void createMapper() {
    mapper = new HamburgMapper();
  }

  @Test
  public void shouldAllowThroughTraffic_WhenAccessCustomers_AndCustomersHVV() {
    OSMWithTags way = new OSMWithTags();
    way.addTag("access", "customers");
    way.addTag("customers", "HVV");

    boolean generalNoThroughTraffic = mapper.isGeneralNoThroughTraffic(way);

    Assertions.assertFalse(generalNoThroughTraffic,
      "access=customers and customers=hvv should not be considered through-traffic");
  }

  @ParameterizedTest
  @CsvSource(value = {
    "no",
    "destination",
    "private",
    "customers",
    "delivery"
  })
  public void shouldDisallowThroughTraffic_WhenNoCustomersHVV(String access) {
    OSMWithTags way = new OSMWithTags();
    way.addTag("access", access);

    boolean generalNoThroughTraffic = mapper.isGeneralNoThroughTraffic(way);

    Assertions.assertTrue(generalNoThroughTraffic,
      "access={no, destination, private, customers, delivery} should be blocked in general");
  }
}
