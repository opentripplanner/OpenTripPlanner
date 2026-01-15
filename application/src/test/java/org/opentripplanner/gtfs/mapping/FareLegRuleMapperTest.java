package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onebusaway.gtfs.model.AgencyAndIdFactory.obaId;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareLegRule;
import org.onebusaway.gtfs.model.FareMedium;
import org.onebusaway.gtfs.model.FareProduct;
import org.onebusaway.gtfs.model.Timeframe;
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
    var ruleMapper = new FareLegRuleMapper(
      ID_FACTORY,
      productMapper,
      timeframeMapper(),
      DataImportIssueStore.NOOP
    );
    var productId = new AgencyAndId("1", "1");
    var fp = new FareProduct();
    fp.setAmount(10);
    fp.setName("Day pass");
    fp.setCurrency("EUR");
    fp.setFareProductId(productId);
    var internalProduct = productMapper.map(fp);

    final var obaRule = baseRule();
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
    var ruleMapper = new FareLegRuleMapper(
      ID_FACTORY,
      productMapper,
      timeframeMapper(),
      DataImportIssueStore.NOOP
    );

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

    var obaRule = baseRule();
    obaRule.setFareProductId(cashProduct.getFareProductId());

    var mappedRules = List.copyOf(ruleMapper.map(List.of(obaRule)));
    assertEquals(1, mappedRules.size());

    var otpRule = mappedRules.get(0);
    assertThat(otpRule.fareProducts()).containsExactly(internalCashProduct, internalCreditProduct);
  }

  @Test
  void priority() {
    var productMapper = new FareProductMapper(ID_FACTORY);
    var ruleMapper = new FareLegRuleMapper(
      ID_FACTORY,
      productMapper,
      timeframeMapper(),
      DataImportIssueStore.NOOP
    );
    var product = cashProduct(null);
    productMapper.map(product);

    final var obaRule = baseRule();
    obaRule.setFareProductId(product.getFareProductId());
    obaRule.setRulePriority(55);

    var mapped = List.copyOf(ruleMapper.map(List.of(obaRule))).getFirst();

    assertThat(mapped.priority()).hasValue(55);
  }

  @Test
  void noProductFound() {
    var issues = new DefaultDataImportIssueStore();
    var ruleMapper = new FareLegRuleMapper(
      ID_FACTORY,
      new FareProductMapper(ID_FACTORY),
      timeframeMapper(),
      issues
    );

    var obaRule = new FareLegRule();
    obaRule.setFareProductId(obaId("notfound"));
    obaRule.setRulePriority(55);

    var mapped = ruleMapper.map(List.of(obaRule));
    assertThat(mapped).isEmpty();

    assertThat(issues.listIssues().stream().map(DataImportIssue::getType)).containsExactly(
      "UnknownFareProductId"
    );
  }

  @Test
  void timeframes() {
    var timeframeMapper = timeframeMapper();
    var productMapper = new FareProductMapper(ID_FACTORY);
    var ruleMapper = new FareLegRuleMapper(
      ID_FACTORY,
      productMapper,
      timeframeMapper,
      DataImportIssueStore.NOOP
    );

    var product = cashProduct(null);
    productMapper.map(product);

    var tfId = new AgencyAndId("1", "tf1");
    var tf = new Timeframe();
    tf.setTimeframeGroupId(tfId);
    tf.setStartTime(LocalTime.NOON);
    tf.setEndTime(LocalTime.NOON.plusHours(1));
    tf.setServiceId("s1");
    timeframeMapper.map(tf);

    var obaRule = baseRule();
    obaRule.setFareProductId(product.getFareProductId());
    obaRule.setFromTimeframeGroupId(tfId);
    obaRule.setToTimeframeGroupId(tfId);

    var mapped = List.copyOf(ruleMapper.map(List.of(obaRule))).getFirst();

    assertEquals("[[12:00-13:00,A:s1]]", toStr(mapped.fromTimeframes()));
    assertEquals("[[12:00-13:00,A:s1]]", toStr(mapped.toTimeframes()));
  }

  private static String toStr(
    Collection<org.opentripplanner.ext.fares.model.Timeframe> timeframes
  ) {
    return timeframes.stream().map(t -> t.toString()).toList().toString();
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

  private static TimeframeMapper timeframeMapper() {
    return new TimeframeMapper(ID_FACTORY);
  }

  private static FareLegRule baseRule() {
    var obaRule = new FareLegRule();
    obaRule.setLegGroupId(obaId("1"));
    return obaRule;
  }
}
