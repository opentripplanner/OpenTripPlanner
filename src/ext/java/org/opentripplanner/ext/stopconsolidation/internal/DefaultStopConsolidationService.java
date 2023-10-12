package org.opentripplanner.ext.stopconsolidation.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
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
    return model
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

  @Override
  public List<FeedScopedId> secondaryStops() {
    return replacements().stream().map(StopReplacement::secondary).toList();
  }

  @Override
  public boolean isPrimaryStop(StopLocation stop) {
    return model.groups().stream().anyMatch(r -> r.primary().equals(stop.getId()));
  }

  @Override
  public boolean isActive() {
    return !model.groups().isEmpty();
  }

  @Override
  public I18NString agencySpecificName(StopLocation stop, Agency agency) {
    if (agency.getId().getFeedId().equals(stop.getId().getFeedId())) {
      return stop.getName();
    } else {
      return model
        .groups()
        .stream()
        .filter(r -> r.primary().equals(stop.getId()))
        .flatMap(g -> g.secondaries().stream())
        .filter(secondary -> secondary.getFeedId().equals(agency.getId().getFeedId()))
        .findAny()
        .map(id -> transitModel.getStopModel().getRegularStop(id))
        .map(RegularStop::getName)
        .orElseGet(stop::getName);
    }
  }
}
