package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareLegRule;
import org.onebusaway.gtfs.model.FareMedium;
import org.onebusaway.gtfs.model.FareProduct;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareDistance.LinearDistance;
import org.opentripplanner.ext.fares.model.FareDistance.Stops;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.model.basic.Distance;

class FareLegRuleMapperTest {

  public static final IdFactory ID_FACTORY = new IdFactory("A");

  private record TestCase(
    Integer distanceType,
    Double minDistance,
    Double maxDistance,
    FareDistance expectedDistance
  ) {}

  private static List<TestCase> testCases() {
    return List.of(
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
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void mapDistance(TestCase tc) {
    var productMapper = new FareProductMapper(ID_FACTORY);
    var ruleMapper = new FareLegRuleMapper(ID_FACTORY, productMapper, DataImportIssueStore.NOOP);
    var productId = new AgencyAndId("1", "1");
    var fp = new FareProduct();
    fp.setAmount(10);
    fp.setName("Day pass");
    fp.setCurrency("EUR");
    fp.setFareProductId(productId);
    var internalProduct = productMapper.map(fp);

    var obaRule = new FareLegRule();
    obaRule.setFareProductId(fp.getFareProductId());
    obaRule.setDistanceType(tc.distanceType);
    obaRule.setMinDistance(tc.minDistance);
    obaRule.setMaxDistance(tc.maxDistance);

    var mappedRules = List.copyOf(ruleMapper.map(List.of(obaRule)));
    assertEquals(1, mappedRules.size());

    var otpRule = mappedRules.get(0);
    assertEquals(otpRule.fareDistance(), tc.expectedDistance);
    assertThat(otpRule.fareProducts()).containsExactly(internalProduct);
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

    final var cashProduct = cashProduct(creditMedium);
    var internalCashProduct = productMapper.map(cashProduct);

    final var creditProduct = cashProduct(cashMedium);
    var internalCreditProduct = productMapper.map(creditProduct);

    var obaRule = new FareLegRule();
    obaRule.setFareProductId(cashProduct.getFareProductId());

    var mappedRules = List.copyOf(ruleMapper.map(List.of(obaRule)));
    assertEquals(1, mappedRules.size());

    var otpRule = mappedRules.get(0);
    assertThat(otpRule.fareProducts()).containsExactly(internalCashProduct, internalCreditProduct);
  }

  @Test
  void priority() {
    var productMapper = new FareProductMapper(ID_FACTORY);
    var ruleMapper = new FareLegRuleMapper(ID_FACTORY, productMapper, DataImportIssueStore.NOOP);
    var product = cashProduct(null);
    productMapper.map(product);

    var obaRule = new FareLegRule();
    obaRule.setFareProductId(product.getFareProductId());
    obaRule.setRulePriority(55);

    var mapped = List.copyOf(ruleMapper.map(List.of(obaRule))).getFirst();

    assertThat(mapped.priority()).hasValue(55);
  }

  @Test
  void noProductFound() {
    var issues = new DefaultDataImportIssueStore();
    var ruleMapper = new FareLegRuleMapper(ID_FACTORY, new FareProductMapper(ID_FACTORY), issues);

    var obaRule = new FareLegRule();
    obaRule.setFareProductId(new AgencyAndId("1", "notfound"));
    obaRule.setRulePriority(55);

    var mapped = ruleMapper.map(List.of(obaRule));
    assertThat(mapped).isEmpty();

    assertThat(issues.listIssues().stream().map(DataImportIssue::getType)).containsExactly(
      "UnknownFareProductId"
    );
  }

  private static FareProduct cashProduct(FareMedium creditMedium) {
    var productId = new AgencyAndId("1", "1");
    var cashProduct = new FareProduct();
    cashProduct.setAmount(10);
    cashProduct.setName("Day pass");
    cashProduct.setCurrency("EUR");
    cashProduct.setFareMedium(creditMedium);
    cashProduct.setFareProductId(productId);
    return cashProduct;
  }
}
