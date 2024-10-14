package org.opentripplanner.ext.stopconsolidation.internal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
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
  public boolean isSecondaryStop(StopLocation stop) {
    return repo.groups().stream().anyMatch(r -> r.secondaries().contains(stop.getId()));
  }

  @Override
  public boolean isPartOfConsolidatedStop(StopLocation sl) {
    return isSecondaryStop(sl) || isPrimaryStop(sl);
  }

  @Override
  public boolean isActive() {
    return !repo.groups().isEmpty();
  }

  @Override
  public StopLocation agencySpecificStop(StopLocation stop, Agency agency) {
    if (agency.getId().getFeedId().equals(stop.getId().getFeedId())) {
      return stop;
    } else {
      return findAgencySpecificStop(stop, agency).orElse(stop);
    }
  }

  @Nonnull
  private Optional<StopLocation> findAgencySpecificStop(StopLocation stop, Agency agency) {
    return repo
      .groups()
      .stream()
      .filter(r -> r.primary().equals(stop.getId()))
      .flatMap(g -> g.secondaries().stream())
      .filter(secondary -> secondary.getFeedId().equals(agency.getId().getFeedId()))
      .findAny()
      .map(id -> copyStopWithName(stop, transitModel.getStopModel().getRegularStop(id).getName()));
  }

  @Nonnull
  private StopLocation copyStopWithName(StopLocation primaryStop, I18NString agencySpecificName) {
    if (primaryStop instanceof RegularStop) {
      return ((RegularStop) primaryStop).copy().withName(agencySpecificName).build();
    }

    if (primaryStop instanceof AreaStop) {
      return ((AreaStop) primaryStop).copy().withName(agencySpecificName).build();
    }

    if (primaryStop instanceof GroupStop) {
      return ((GroupStop) primaryStop).copy().withName(agencySpecificName).build();
    }

    // should not happen unless new stop types are added
    throw new IllegalArgumentException("Unknown stop location type: " + primaryStop);
  }

  @Override
  public Optional<StopLocation> primaryStop(FeedScopedId id) {
    var primaryId = repo
      .groups()
      .stream()
      .filter(g -> g.secondaries().contains(id))
      .map(ConsolidatedStopGroup::primary)
      .findAny()
      .orElse(id);
    return Optional.ofNullable(transitModel.getStopModel().getRegularStop(primaryId));
  }
}
