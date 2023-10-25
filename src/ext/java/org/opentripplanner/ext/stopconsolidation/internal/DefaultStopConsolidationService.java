package org.opentripplanner.ext.stopconsolidation.internal;

import java.util.List;
import java.util.Objects;
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

public class DefaultStopConsolidationService implements StopConsolidationService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultStopConsolidationService.class);

  private final StopConsolidationRepository repo;
  private final TransitModel transitModel;

  public DefaultStopConsolidationService(
    StopConsolidationRepository repo,
    TransitModel transitModel
  ) {
    this.repo = Objects.requireNonNull(repo);
    this.transitModel = Objects.requireNonNull(transitModel);
  }

  @Override
  public List<StopReplacement> replacements() {
    return repo
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
    return repo.groups().stream().anyMatch(r -> r.primary().equals(stop.getId()));
  }

  @Override
  public boolean isActive() {
    return !repo.groups().isEmpty();
  }

  @Override
  public I18NString agencySpecificName(StopLocation stop, Agency agency) {
    if (agency.getId().getFeedId().equals(stop.getId().getFeedId())) {
      return stop.getName();
    } else {
      return repo
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
