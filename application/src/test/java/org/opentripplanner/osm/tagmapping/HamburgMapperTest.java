package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.osm.model.OsmWay;

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
    var way = OsmWay.of().withTag("access", "customers").withTag("customers", "HVV").build();

    boolean generalNoThroughTraffic = mapper.isGeneralNoThroughTraffic(way);

    assertFalse(
      generalNoThroughTraffic,
      "access=customers and customers=hvv should allow through-traffic"
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "no", "destination", "private", "customers", "delivery" })
  public void shouldDisallowThroughTraffic_WhenNoCustomersHVV(String access) {
    var way = OsmWay.of().withTag("access", access).build();

    boolean generalNoThroughTraffic = mapper.isGeneralNoThroughTraffic(way);

    assertTrue(
      generalNoThroughTraffic,
      "access={no, destination, private, customers, delivery} should be blocked in general"
    );
  }
}
