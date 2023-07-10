package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.ext.fares.model.Distance;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.fare.FareProduct;

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
        FareDistance fareDistance = createFareDistance(r);

        if (productForRule != null) {
          return FareLegRule
            .of(productForRule)
            .withLegGroupId(r.getLegGroupId())
            .withNetworkId(r.getNetworkId())
            .withFromAreaId(r.getFromAreaId())
            .withToAreaId(r.getToAreaId())
            .withFareDistance(fareDistance)
            .build();
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

  private FareDistance createFareDistance(org.onebusaway.gtfs.model.FareLegRule fareLegRule) {
    final Integer distanceType = fareLegRule.getDistanceType();
    if (distanceType == null) {
      return null;
    }
    return switch (distanceType) {
      case 0 -> new FareDistance.Stops(
        fareLegRule.getMinDistance().intValue(),
        fareLegRule.getMaxDistance().intValue()
      );
      case 1 -> new FareDistance.LinearDistance(
        Distance.ofMeters(fareLegRule.getMinDistance()),
        Distance.ofMeters(fareLegRule.getMaxDistance())
      );
      default -> null;
    };
  }
}
