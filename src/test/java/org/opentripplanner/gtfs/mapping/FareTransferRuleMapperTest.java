package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareProduct;
import org.onebusaway.gtfs.model.FareTransferRule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;

class FareTransferRuleMapperTest {

  final String feedId = "A";
  final String productId = "123";
  final AgencyAndId id = new AgencyAndId(feedId, productId);
  final AgencyAndId groupId1 = new AgencyAndId(feedId, "group1");
  final AgencyAndId groupId2 = new AgencyAndId(feedId, "group2");

  @Test
  void addIssueForUnknownProduct() {
    var fareProductMapper = new FareProductMapper();
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new FareTransferRuleMapper(fareProductMapper, issueStore);

    var rule = new FareTransferRule();
    rule.setFareProductId(id);

    var mapped = subject.map(List.of(rule));

    assertTrue(mapped.isEmpty());
    assertEquals("UnknownFareProductId", issueStore.listIssues().get(0).getType());
  }

  @Test
  void transferRuleWithoutLegGroups() {
    var fareProduct = new FareProduct();
    fareProduct.setId(id);
    fareProduct.setFareProductId(id);
    fareProduct.setCurrency("EUR");
    fareProduct.setAmount(1000);

    var rule = new FareTransferRule();
    rule.setFareProductId(id);

    var transferRule = map(fareProduct, rule);
    assertEquals(feedId, transferRule.feedId());
    assertNull(transferRule.fromLegGroup());
    assertNull(transferRule.toLegGroup());
  }

  @Test
  void transferRuleWithLegGroup() {
    var fareProduct = new FareProduct();
    fareProduct.setId(id);
    fareProduct.setFareProductId(id);
    fareProduct.setCurrency("EUR");
    fareProduct.setAmount(1000);

    var rule = new FareTransferRule();
    rule.setFareProductId(id);
    rule.setFromLegGroupId(groupId1);
    rule.setToLegGroupId(groupId2);

    var transferRule = map(fareProduct, rule);

    assertEquals(groupId1.getId(), transferRule.fromLegGroup());
    assertEquals(groupId2.getId(), transferRule.toLegGroup());
  }

  private org.opentripplanner.ext.fares.model.FareTransferRule map(
    FareProduct fareProduct,
    FareTransferRule rule
  ) {
    var fareProductMapper = new FareProductMapper();
    fareProductMapper.map(fareProduct);

    var subject = new FareTransferRuleMapper(fareProductMapper, DataImportIssueStore.NOOP);

    var mapped = subject.map(List.of(rule)).stream().toList();

    assertFalse(mapped.isEmpty());

    return mapped.get(0);
  }
}
