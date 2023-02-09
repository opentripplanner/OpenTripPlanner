package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.ext.fares.model.Distance;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

public final class FareLegRuleMapper {

  private final FareProductMapper fareProductMapper;
  private final DataImportIssueStore issueStore;

  public FareLegRuleMapper(FareProductMapper fareProductMapper, DataImportIssueStore issueStore) {
    this.fareProductMapper = fareProductMapper;
    this.issueStore = issueStore;
  }

  public Collection<FareLegRule> map(
    Collection<org.onebusaway.gtfs.model.FareLegRule> allFareLegRules
  ) {
    return allFareLegRules
      .stream()
      .map(r -> {
        FareProduct productForRule = fareProductMapper.map(r.getFareProduct());
        FareDistance fareDistance =
          switch (r.getDistanceType()) {
            case 0 -> new FareDistance.Stops(
              r.getMinDistance().intValue(),
              r.getMaxDistance().intValue()
            );
            case 1 -> new FareDistance.LinearDistance(
              Distance.ofMeters(r.getMinDistance()),
              Distance.ofMeters(r.getMaxDistance())
            );
            default -> null;
          };

        if (productForRule != null) {
          return new FareLegRule(
            r.getLegGroupId(),
            r.getNetworkId(),
            r.getFromAreaId(),
            r.getToAreaId(),
            fareDistance,
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
