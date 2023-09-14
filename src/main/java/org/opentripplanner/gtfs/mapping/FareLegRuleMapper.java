package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.ext.fares.model.Distance;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;

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
        var fareProductId = mapAgencyAndId(r.getFareProductId());
        var productsForRule = fareProductMapper.getByFareProductId(fareProductId);

        if (!productsForRule.isEmpty()) {
          FareDistance fareDistance = createFareDistance(r);
          var ruleId = new FeedScopedId(fareProductId.getFeedId(), r.getId());
          return FareLegRule
            .of(ruleId, productsForRule)
            .withLegGroupId(mapAgencyAndId(r.getLegGroupId()))
            .withNetworkId(r.getNetworkId())
            .withFromAreaId(areaId(r.getFromArea()))
            .withToAreaId(areaId(r.getToArea()))
            .withFareDistance(fareDistance)
            .build();
        } else {
          issueStore.add(
            "UnknownFareProductId",
            "Fare leg rule %s refers to unknown fare product %s",
            r.getId(),
            fareProductId
          );
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();
  }

  private static String areaId(org.onebusaway.gtfs.model.Area area) {
    if (area == null) {
      return null;
    } else {
      return area.getAreaId();
    }
  }

  private static FareDistance createFareDistance(
    org.onebusaway.gtfs.model.FareLegRule fareLegRule
  ) {
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
