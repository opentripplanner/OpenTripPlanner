package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nullable;
import org.onebusaway.gtfs.model.Area;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.basic.Distance;
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
    return allFareLegRules.stream().map(this::map).filter(Objects::nonNull).toList();
  }

  @Nullable
  private FareLegRule map(org.onebusaway.gtfs.model.FareLegRule r) {
    var fareProductId = idFactory.createId(r.getFareProductId(), "fare leg rule's fare product id");
    var productsForRule = fareProductMapper.findFareProducts(fareProductId);

    if (!productsForRule.isEmpty()) {
      FareDistance fareDistance = createFareDistance(r);
      var ruleId = idFactory.createId(r.getId(), "fare leg rule");
      var builder = FareLegRule.of(ruleId, productsForRule)
        .withLegGroupId(idFactory.createNullableId(r.getLegGroupId()))
        .withNetworkId(idFactory.createNullableId(r.getNetworkId()))
        .withFromAreaId(areaId(r.getFromArea()))
        .withToAreaId(areaId(r.getToArea()))
        .withFareDistance(fareDistance);
      r.getRulePriorityOption().ifPresent(builder::withPriority);
      return builder.build();
    } else {
      issueStore.add(
        "UnknownFareProductId",
        "Fare leg rule %s refers to unknown fare product %s",
        r.getId(),
        fareProductId
      );
      return null;
    }
  }

  private FeedScopedId areaId(@Nullable Area area) {
    if (area == null) {
      return null;
    } else {
      return idFactory.createId(area.getAreaId(), "area");
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
