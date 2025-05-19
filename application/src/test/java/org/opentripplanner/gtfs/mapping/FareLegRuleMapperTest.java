package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareLegRule;
import org.onebusaway.gtfs.model.FareMedium;
import org.onebusaway.gtfs.model.FareProduct;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareDistance.LinearDistance;
import org.opentripplanner.ext.fares.model.FareDistance.Stops;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.basic.Distance;

class FareLegRuleMapperTest {

  public static final IdFactory ID_FACTORY = new IdFactory("A");

  private record TestCase(
    Integer distanceType,
    Double minDistance,
    Double maxDistance,
    FareDistance expectedDistance
  ) {}

  private final List<TestCase> testCases = List.of(
    new TestCase(0, 1d, 10d, new Stops(1, 10)),
    new TestCase(
      1,
      5000d,
      10000d,
      new LinearDistance(
        Distance.ofKilometersBoxed(5d, ignore -> {}).orElse(null),
        Distance.ofKilometersBoxed(10d, ignore -> {}).orElse(null)
      )
    ),
    new TestCase(null, null, null, null)
  );

  @TestFactory
  Stream<DynamicTest> mapDistance() {
    var productMapper = new FareProductMapper(ID_FACTORY);
    var ruleMapper = new FareLegRuleMapper(ID_FACTORY, productMapper, DataImportIssueStore.NOOP);
    var productId = new AgencyAndId("1", "1");
    var fp = new FareProduct();
    fp.setAmount(10);
    fp.setName("Day pass");
    fp.setCurrency("EUR");
    fp.setFareProductId(productId);
    var internalProduct = productMapper.map(fp);

    return testCases
      .stream()
      .map(tc ->
        dynamicTest(tc.toString(), () -> {
          var obaRule = new FareLegRule();
          obaRule.setFareProductId(fp.getFareProductId());
          obaRule.setDistanceType(tc.distanceType);
          obaRule.setMinDistance(tc.minDistance);
          obaRule.setMaxDistance(tc.maxDistance);

          var mappedRules = List.copyOf(ruleMapper.map(List.of(obaRule)));
          assertEquals(1, mappedRules.size());

          var otpRule = mappedRules.get(0);
          assertEquals(otpRule.fareDistance(), tc.expectedDistance);
          assert (otpRule.fareProducts().size() == 1);
          assert (otpRule.fareProducts().contains(internalProduct));
        })
      );
  }

  @Test
  void multipleProducts() {
    var productMapper = new FareProductMapper(ID_FACTORY);
    var ruleMapper = new FareLegRuleMapper(ID_FACTORY, productMapper, DataImportIssueStore.NOOP);

    var cashMedium = new FareMedium();
    cashMedium.setId(new AgencyAndId("1", "cash"));
    cashMedium.setName("Cash");
    cashMedium.setFareMediaType(0);

    var creditMedium = new FareMedium();
    creditMedium.setId(new AgencyAndId("1", "credit"));
    creditMedium.setName("Credit");
    creditMedium.setFareMediaType(0);

    var productId = new AgencyAndId("1", "1");

    var cashProduct = new FareProduct();
    cashProduct.setAmount(10);
    cashProduct.setName("Day pass");
    cashProduct.setCurrency("EUR");
    cashProduct.setFareMedium(creditMedium);
    cashProduct.setFareProductId(productId);
    var internalCashProduct = productMapper.map(cashProduct);

    var creditProduct = new FareProduct();
    creditProduct.setAmount(10);
    creditProduct.setName("Day pass");
    creditProduct.setCurrency("EUR");
    creditProduct.setFareMedium(cashMedium);
    creditProduct.setFareProductId(productId);
    var internalCreditProduct = productMapper.map(creditProduct);

    var obaRule = new FareLegRule();
    obaRule.setFareProductId(productId);

    var mappedRules = List.copyOf(ruleMapper.map(List.of(obaRule)));
    assertEquals(1, mappedRules.size());

    var otpRule = mappedRules.get(0);
    assert (otpRule.fareProducts().size() == 2);
    assert (otpRule.fareProducts().contains(internalCashProduct));
    assert (otpRule.fareProducts().contains(internalCreditProduct));
  }

  @Test
  void noProducts() {
    var productMapper = new FareProductMapper(ID_FACTORY);
    var ruleMapper = new FareLegRuleMapper(ID_FACTORY, productMapper, DataImportIssueStore.NOOP);
    var obaRule = new FareLegRule();
    var mappedRules = List.copyOf(ruleMapper.map(List.of(obaRule)));
    assertEquals(0, mappedRules.size());
  }
}
