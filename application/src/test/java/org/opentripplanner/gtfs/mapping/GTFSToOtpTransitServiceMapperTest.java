package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.NOOP;

import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareLegRule;
import org.onebusaway.gtfs.model.FareProduct;
import org.onebusaway.gtfs.model.FareRule;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;

class GTFSToOtpTransitServiceMapperTest {

  private static final AgencyAndId OBA_ID = new AgencyAndId("f", "1");

  @Test
  void faresV1only() {
    var mapper = mapper();
    var fareData = mapper.fareRulesData();
    assertThat(fareData.fareRules()).isEmpty();
    assertThat(fareData.fareAttributes()).isEmpty();

    var dao = new GtfsRelationalDaoImpl();
    dao.saveEntity(fareRule());
    dao.saveEntity(fareAttribute());
    assertTrue(dao.hasFaresV1());
    assertFalse(dao.hasFaresV2());

    mapper.mapStopTripAndRouteDataIntoBuilder(dao);

    assertThat(fareData.fareRules()).isNotEmpty();
    assertThat(fareData.fareAttributes()).isNotEmpty();
  }

  /**
   * Tests that if you have both V1 and V2 fares, the V1 fares are ignored.
   */
  @Test
  void faresV1andV2() {
    OTPFeature.FaresV2.testOn(() -> {
      var mapper = mapper();
      var fareData = mapper.fareRulesData();

      var dao = new GtfsRelationalDaoImpl();
      dao.saveEntity(fareRule());
      dao.saveEntity(fareAttribute());
      dao.saveEntity(fareProduct());
      dao.saveEntity(fareLegRule());

      mapper.mapStopTripAndRouteDataIntoBuilder(dao);

      assertThat(fareData.fareRules()).isEmpty();
      assertThat(fareData.fareAttributes()).isEmpty();
      assertThat(fareData.fareLegRules()).isNotEmpty();
    });
  }

  private static GTFSToOtpTransitServiceMapper mapper() {
    var builder = new OtpTransitServiceBuilder(SiteRepository.of().build(), NOOP);
    return new GTFSToOtpTransitServiceMapper(
      builder,
      "f",
      NOOP,
      true,
      StopTransferPriority.PREFERRED
    );
  }

  private static FareProduct fareProduct() {
    var p = new FareProduct();
    p.setId(OBA_ID);
    p.setName("A fare product");
    p.setFareProductId(OBA_ID);
    p.setCurrency("EUR");
    p.setAmount(10);
    return p;
  }

  private static FareLegRule fareLegRule() {
    var f = new FareLegRule();
    f.setFareProductId(OBA_ID);
    f.setLegGroupId(OBA_ID);
    return f;
  }

  private static FareRule fareRule() {
    var r = new FareRule();
    r.setId(1);
    r.setFare(fareAttribute());
    return r;
  }

  private static FareAttribute fareAttribute() {
    var a = new FareAttribute();
    a.setId(OBA_ID);
    a.setPrice(1);
    a.setCurrencyType("EUR");
    return a;
  }
}
