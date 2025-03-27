package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FareTransferRuleMapper {

  public static final FareProduct FREE_TRANSFER = FareProduct.of(new FeedScopedId("unknown", "free-transfer"), "Free transfer", Money.ZERO_USD).build();
  public final int MISSING_VALUE = -999;

  private final DataImportIssueStore issueStore;
  private final FareProductMapper fareProductMapper;

  public FareTransferRuleMapper(
    FareProductMapper fareProductMapper,
    DataImportIssueStore issueStore
  ) {
    this.fareProductMapper = fareProductMapper;
    this.issueStore = issueStore;
  }

  public Collection<FareTransferRule> map(
    Collection<org.onebusaway.gtfs.model.FareTransferRule> allRules
  ) {
    return allRules.stream().map(this::doMap).filter(Objects::nonNull).toList();
  }

  private FareTransferRule doMap(org.onebusaway.gtfs.model.FareTransferRule rhs) {
    var fareProductId = mapAgencyAndId(rhs.getFareProductId());
    final var products = getFareProducts(fareProductId, rhs.getId());
    if (products == null) {
      return null;
    }

    Duration duration = null;
    if (rhs.getDurationLimit() != MISSING_VALUE) {
      duration = Duration.ofSeconds(rhs.getDurationLimit());
    }
    return new FareTransferRule(
      new FeedScopedId(fareProductId.getFeedId(), rhs.getId()),
      AgencyAndIdMapper.mapAgencyAndId(rhs.getFromLegGroupId()),
      AgencyAndIdMapper.mapAgencyAndId(rhs.getToLegGroupId()),
      rhs.getTransferCount(),
      duration,
      products
    );
  }

  @Nullable
  private Collection<FareProduct> getFareProducts(@Nullable FeedScopedId fareProductId, String id) {
    if(fareProductId == null) {
      return List.of(FREE_TRANSFER);
    }
    var products = fareProductMapper.getByFareProductId(fareProductId);
    if (products.isEmpty()) {
      issueStore.add(
        "UnknownFareProductId",
        "Fare product with id %s referenced by fare transfer rule with id %s not found.".formatted(
          fareProductId,
          id
          )
      );
      return null;
    }
    return products;
  }
}
