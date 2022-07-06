package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FareLegRule;
import org.opentripplanner.model.FareProduct;

public record FareLegRuleMapper(
  FareProductMapper fareProductMapper,
  DataImportIssueStore issueStore
) {
  public Collection<FareLegRule> map(
    Collection<org.onebusaway.gtfs.model.FareLegRule> allFareLegRules
  ) {
    return allFareLegRules
      .stream()
      .map(r -> {
        FareProduct productForRule = fareProductMapper.map(r.getFareProduct());
        if (productForRule != null) {
          return new FareLegRule(
            productForRule.id().getFeedId(),
            r.getNetworkId(),
            r.getFromAreaId(),
            r.getToAreaId(),
            productForRule
          );
        } else {
          issueStore.add(
            "UnknownFareProductId",
            "Fare leg rule %s refers to unknown fare product %s",
            r.getId(),
            r.getFareProduct().getId()
          );
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();
  }
}
