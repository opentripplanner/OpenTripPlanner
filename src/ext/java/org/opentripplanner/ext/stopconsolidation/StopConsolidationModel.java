package org.opentripplanner.ext.stopconsolidation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationParser.StopGroup;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class StopConsolidationModel implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(StopConsolidationModel.class);
  private final TransitModel transitModel;
  private final List<StopGroup> groups;

  // lazily initialized because the stop model is not loaded at construction time
  private List<StopReplacement> replacements;

  @Inject
  public StopConsolidationModel(TransitModel transitModel) {
    try {
      groups = StopConsolidationParser.parseGroups();

      LOG.info("Parsed {} consolidated stop groups", groups.size());
    } finally {
      this.transitModel = transitModel;
    }
  }

  public List<FeedScopedId> stopIdsToReplace() {
    return replacements().stream().map(StopReplacement::secondary).toList();
  }

  public List<StopReplacement> replacements() {
    return lazilyGetReplacements();
  }

  @Nonnull
  private synchronized List<StopReplacement> lazilyGetReplacements() {
    if (replacements == null) {

      replacements = groups
        .stream()
        .flatMap(group -> {
          var primaryStop = transitModel.getStopModel().getRegularStop(group.primary());
          if (primaryStop == null) {
            LOG.error("Could not find primary stop with id {}. Ignoring stop group {}.", group.primary(), group);
            return Stream.empty();
          } else {
            return group.secondaries().stream().map(r -> new StopReplacement(primaryStop, r));
          }
        })
        .toList();
    }
    return replacements;
  }

  public boolean isSecondaryStop(StopLocation stop) {
    return groups.stream().anyMatch(r -> r.secondaries().contains(stop.getId()));
  }

  private Optional<I18NString> primaryName(StopLocation secondary) {
    return groups
      .stream()
      .filter(r -> r.secondaries().contains(secondary.getId()))
      .findAny()
      .map(StopGroup::primary)
      .map(id -> transitModel.getStopModel().getRegularStop(id))
      .map(RegularStop::getName);
  }

  public I18NString agencySpecificName(StopLocation stop) {
    return primaryName(stop).orElse(stop.getName());
  }

  public record StopReplacement(StopLocation primary, FeedScopedId secondary) {}
}
