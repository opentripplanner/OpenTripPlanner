package org.opentripplanner.ext.stopconsolidation.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultStopConsolidationService implements StopConsolidationService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultStopConsolidationService.class);

  private final StopConsolidationRepository model;
  private final TransitModel transitModel;

  // lazily initialized because the stop model is not loaded at construction time
  private List<StopReplacement> replacements;

  @Inject
  public DefaultStopConsolidationService(
    StopConsolidationRepository model,
    TransitModel transitModel
  ) {
    this.model = model;
    this.transitModel = transitModel;
  }

  @Override
  public List<StopReplacement> replacements() {
    return lazilyGetReplacements();
  }

  @Override
  public List<FeedScopedId> stopIdsToReplace() {
    return replacements().stream().map(StopReplacement::secondary).toList();
  }

  @Override
  public boolean isSecondaryStop(StopLocation stop) {
    return model.groups().stream().anyMatch(r -> r.secondaries().contains(stop.getId()));
  }

  @Override
  public boolean isActive() {
    return !model.groups().isEmpty();
  }

  @Nonnull
  private synchronized List<StopReplacement> lazilyGetReplacements() {
    if (replacements == null) {
      replacements =
        model
          .groups()
          .stream()
          .flatMap(group -> {
            var primaryStop = transitModel.getStopModel().getRegularStop(group.primary());
            if (primaryStop == null) {
              LOG.error(
                "Could not find primary stop with id {}. Ignoring stop group {}.",
                group.primary(),
                group
              );
              return Stream.empty();
            } else {
              return group.secondaries().stream().map(r -> new StopReplacement(primaryStop, r));
            }
          })
          .toList();
    }
    return replacements;
  }

  private Optional<I18NString> primaryName(StopLocation secondary) {
    return model
      .groups()
      .stream()
      .filter(r -> r.secondaries().contains(secondary.getId()))
      .findAny()
      .map(ConsolidatedStopGroup::primary)
      .map(id -> transitModel.getStopModel().getRegularStop(id))
      .map(RegularStop::getName);
  }

  @Override
  public I18NString agencySpecificName(StopLocation stop) {
    return primaryName(stop).orElse(stop.getName());
  }
}
