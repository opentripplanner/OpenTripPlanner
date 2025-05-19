package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareProduct;
import org.onebusaway.gtfs.model.FareTransferRule;

class FareTransferRuleMapperTest {

  private static final String FEED_ID = "A";
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);
  final String productId = "123";
  final AgencyAndId id = new AgencyAndId(FEED_ID, productId);
  final AgencyAndId groupId1 = new AgencyAndId(FEED_ID, "group1");
  final AgencyAndId groupId2 = new AgencyAndId(FEED_ID, "group2");

  @Test
  void throwOnUnknownFareProduct() {
    var fareProductMapper = new FareProductMapper(ID_FACTORY);
    var subject = new FareTransferRuleMapper(ID_FACTORY, fareProductMapper);

    var rule = new FareTransferRule();
    rule.setFareProductId(id);

    assertThrows(IllegalArgumentException.class, () -> subject.map(List.of(rule)));
  }

  @Test
  void transferRuleWithoutLegGroups() {
    var fareProduct = fareProduct();

    var rule = new FareTransferRule();
    rule.setFareProductId(id);

    var transferRule = map(fareProduct, rule);
    assertEquals(FEED_ID, transferRule.feedId());
    assertNull(transferRule.fromLegGroup());
    assertNull(transferRule.toLegGroup());
  }

  @Test
  void transferRuleWithLegGroup() {
    var fareProduct = fareProduct();

    var rule = new FareTransferRule();
    rule.setFareProductId(id);
    rule.setFromLegGroupId(groupId1);
    rule.setToLegGroupId(groupId2);

    var transferRule = map(fareProduct, rule);

    assertEquals(groupId1.getId(), transferRule.fromLegGroup().getId());
    assertEquals(groupId2.getId(), transferRule.toLegGroup().getId());
  }

  @Test
  void ruleWithoutProductIsFree() {
    var rule = new FareTransferRule();
    rule.setFromLegGroupId(groupId1);
    rule.setToLegGroupId(groupId2);

    var fareProductMapper = new FareProductMapper(ID_FACTORY);
    var subject = new FareTransferRuleMapper(ID_FACTORY, fareProductMapper);
    var transferRule = subject.map(List.of(rule)).stream().toList().getFirst();
    assertTrue(transferRule.isFree());
    assertThat(transferRule.fareProducts()).isEmpty();
  }

  private FareProduct fareProduct() {
    var fareProduct = new FareProduct();
    fareProduct.setId(id);
    fareProduct.setName("A fare product");
    fareProduct.setFareProductId(id);
    fareProduct.setCurrency("EUR");
    fareProduct.setAmount(1000);
    return fareProduct;
  }

  private org.opentripplanner.ext.fares.model.FareTransferRule map(
    FareProduct fareProduct,
    FareTransferRule rule
  ) {
    var fareProductMapper = new FareProductMapper(ID_FACTORY);
    fareProductMapper.map(fareProduct);

    var subject = new FareTransferRuleMapper(ID_FACTORY, fareProductMapper);

    var mapped = subject.map(List.of(rule)).stream().toList();

    assertFalse(mapped.isEmpty());

    return mapped.getFirst();
  }
}
