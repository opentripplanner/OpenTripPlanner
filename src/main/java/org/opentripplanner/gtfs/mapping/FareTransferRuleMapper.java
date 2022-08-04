package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FareTransferRuleMapper {

  private final Map<FeedScopedId, FareProduct> fareProductsById;

  public FareTransferRuleMapper(List<FareProduct> fareProducts) {
    this.fareProductsById = Maps.uniqueIndex(fareProducts, FareProduct::id);
  }

  public Collection<FareLegRule> map(
    Collection<org.onebusaway.gtfs.model.FareTransferRule> allRules
  ) {
    return allRules.stream().map(r -> null).filter(Objects::nonNull).toList();
  }
}
