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

    return fareProductMapper
      .getByFareProductId(fareProductId)
      .map(p -> {
        Duration duration = null;
        if (rhs.getDurationLimit() != MISSING_VALUE) {
          duration = Duration.ofSeconds(rhs.getDurationLimit());
        }
        return new FareTransferRule(
          mapAgencyAndId(rhs.getFromLegGroupId()),
          mapAgencyAndId(rhs.getToLegGroupId()),
          rhs.getTransferCount(),
          duration,
          p
        );
      })
      .orElseGet(() -> {
        issueStore.add(
          "UnknownFareProductId",
          "Fare product with id %s referenced by fare transfer rule with id %s not found.".formatted(
              fareProductId,
              rhs.getId()
            )
        );
        return null;
      });
  }
}
