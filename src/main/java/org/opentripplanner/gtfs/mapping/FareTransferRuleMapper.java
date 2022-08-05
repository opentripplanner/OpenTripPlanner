package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import com.google.common.collect.Maps;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FareTransferRuleMapper {

  public final int MISSING_VALUE = -999;

  private final Map<FeedScopedId, FareProduct> fareProductsById;
  private final DataImportIssueStore issueStore;

  public FareTransferRuleMapper(List<FareProduct> fareProducts, DataImportIssueStore issueStore) {
    this.fareProductsById = Maps.uniqueIndex(fareProducts, FareProduct::id);
    this.issueStore = issueStore;
  }

  public Collection<FareTransferRule> map(
    Collection<org.onebusaway.gtfs.model.FareTransferRule> allRules
  ) {
    return allRules.stream().map(this::doMap).filter(Objects::nonNull).toList();
  }

  private FareTransferRule doMap(org.onebusaway.gtfs.model.FareTransferRule rhs) {
    Duration duration = null;
    if (rhs.getDurationLimit() != MISSING_VALUE) {
      duration = Duration.ofSeconds(rhs.getDurationLimit());
    }
    var fareProductId = mapAgencyAndId(rhs.getFareProductId());
    var fareProduct = fareProductsById.get(fareProductId);
    if (fareProduct == null) {
      issueStore.add(
        "UnknownFareProductId",
        "Fare product with id %s of fare transfer rule with %s not found.".formatted(
            fareProductId,
            rhs.getId()
          )
      );
      return null;
    }
    return new FareTransferRule(
      mapAgencyAndId(rhs.getFromLegGroupId()),
      mapAgencyAndId(rhs.getToLegGroupId()),
      rhs.getTransferCount(),
      duration,
      fareProduct
    );
  }
}
