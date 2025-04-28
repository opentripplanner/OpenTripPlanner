package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FareLegRuleMapper {

  private static final Logger LOG = LoggerFactory.getLogger(FareLegRuleMapper.class);

  private final IdFactory idFactory;
  private final FareProductMapper fareProductMapper;
  private final DataImportIssueStore issueStore;

  public FareLegRuleMapper(
    IdFactory idFactory,
    FareProductMapper fareProductMapper,
    DataImportIssueStore issueStore
  ) {
    this.idFactory = idFactory;
    this.fareProductMapper = fareProductMapper;
    this.issueStore = issueStore;
  }

  public Collection<FareLegRule> map(
    Collection<org.onebusaway.gtfs.model.FareLegRule> allFareLegRules
  ) {
    return allFareLegRules
      .stream()
      .map(r -> {
        var fareProductId = idFactory.createId(r.getFareProductId());
        var productsForRule = fareProductMapper.getByFareProductId(fareProductId);

        if (!productsForRule.isEmpty()) {
          FareDistance fareDistance = createFareDistance(r);
          var ruleId = new FeedScopedId(fareProductId.getFeedId(), r.getId());
          return FareLegRule.of(ruleId, productsForRule)
            .withLegGroupId(idFactory.createId(r.getLegGroupId()))
            .withNetworkId(idFactory.createNullableId(r.getNetworkId()))
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

  private FeedScopedId areaId(@Nullable org.onebusaway.gtfs.model.Area area) {
    if (area == null) {
      return null;
    } else {
      return idFactory.createId(area.getAreaId());
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
        Distance.ofMetersBoxed(fareLegRule.getMinDistance(), error ->
          LOG.warn(
            "Fare leg rule min distance not valid: {} - {}",
            fareLegRule.getMinDistance(),
            error
          )
        ).orElse(null),
        Distance.ofMetersBoxed(fareLegRule.getMaxDistance(), error ->
          LOG.warn(
            "Fare leg rule max distance not valid: {} - {}",
            fareLegRule.getMaxDistance(),
            error
          )
        ).orElse(null)
      );
      default -> null;
    };
  }
}
