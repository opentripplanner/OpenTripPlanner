package org.opentripplanner.openstreetmap.tagmapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * @author Maintained by HBT (geofox-team@hbt.de)
 */
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

    assertFalse(
      generalNoThroughTraffic,
      "access=customers and customers=hvv should allow through-traffic"
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "no", "destination", "private", "customers", "delivery" })
  public void shouldDisallowThroughTraffic_WhenNoCustomersHVV(String access) {
    OSMWithTags way = new OSMWithTags();
    way.addTag("access", access);

    boolean generalNoThroughTraffic = mapper.isGeneralNoThroughTraffic(way);

    assertTrue(
      generalNoThroughTraffic,
      "access={no, destination, private, customers, delivery} should be blocked in general"
    );
  }
}
