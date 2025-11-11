package org.opentripplanner.gtfs.mapping;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimitType;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FareTransferRuleMapper {

  public final int MISSING_VALUE = -999;

  private final IdFactory idFactory;
  private final FareProductMapper fareProductMapper;

  FareTransferRuleMapper(IdFactory idFactory, FareProductMapper fareProductMapper) {
    this.idFactory = idFactory;
    this.fareProductMapper = fareProductMapper;
  }

  public Collection<FareTransferRule> map(
    Collection<org.onebusaway.gtfs.model.FareTransferRule> allRules
  ) {
    return allRules.stream().map(this::doMap).filter(Objects::nonNull).toList();
  }

  private FareTransferRule doMap(org.onebusaway.gtfs.model.FareTransferRule rhs) {
    var fareProductId = idFactory.createNullableId(rhs.getFareProductId());
    final var products = findFareProducts(fareProductId, rhs.getId());

    var builder = FareTransferRule.of()
      .withId(idFactory.createId(rhs.getId(), "fare transfer rule"))
      .withFromLegGroup(idFactory.createNullableId(rhs.getFromLegGroupId()))
      .withToLegGroup(idFactory.createNullableId(rhs.getToLegGroupId()))
      .withTransferCount(rhs.getTransferCount())
      .withFareProducts(products);
    if (rhs.getDurationLimit() != MISSING_VALUE) {
      var duration = Duration.ofSeconds(rhs.getDurationLimit());
      var type = mapLimitType(rhs.getDurationLimitType());
      builder.withTimeLimit(type, duration);
    }
    return builder.build();
  }

  private Collection<FareProduct> findFareProducts(
    @Nullable FeedScopedId fareProductId,
    String ruleId
  ) {
    // as per the GTFS Fares V2 spec an empty product id means that the transfer is free
    if (fareProductId == null) {
      return List.of();
    }
    var products = fareProductMapper.findFareProducts(fareProductId);
    if (products.isEmpty()) {
      throw new IllegalArgumentException(
        "Cannot find fare product '%s' for transfer rule '%s'".formatted(fareProductId, ruleId)
      );
    }
    return products;
  }

  static TimeLimitType mapLimitType(int durationLimitType) {
    return switch (durationLimitType) {
      case 0 -> TimeLimitType.DEPARTURE_TO_ARRIVAL;
      case 1 -> TimeLimitType.DEPARTURE_TO_DEPARTURE;
      case 2, 3 -> throw new IllegalArgumentException(
        "Duration limit type %s not implemented.".formatted(durationLimitType)
      );
      default -> throw new IllegalArgumentException("Valid duration limit type must be provided.");
    };
  }
}
