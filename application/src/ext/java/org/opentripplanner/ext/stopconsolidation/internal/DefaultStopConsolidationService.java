package org.opentripplanner.ext.stopconsolidation.internal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultStopConsolidationService implements StopConsolidationService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultStopConsolidationService.class);

  private final StopConsolidationRepository repo;
  private final TimetableRepository timetableRepository;

  public DefaultStopConsolidationService(
    StopConsolidationRepository repo,
    TimetableRepository timetableRepository
  ) {
    this.repo = Objects.requireNonNull(repo);
    this.timetableRepository = Objects.requireNonNull(timetableRepository);
  }

  @Override
  public List<StopReplacement> replacements() {
    return repo
      .groups()
      .stream()
      .flatMap(group -> {
        var primaryStop = timetableRepository.getSiteRepository().getRegularStop(group.primary());
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
  public boolean isPartOfConsolidatedStop(@Nullable StopLocation sl) {
    if (sl == null) {
      return false;
    } else {
      return isSecondaryStop(sl) || isPrimaryStop(sl);
    }
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

  private Optional<StopLocation> findAgencySpecificStop(StopLocation stop, Agency agency) {
    return repo
      .groups()
      .stream()
      .filter(r -> r.primary().equals(stop.getId()))
      .flatMap(g -> g.secondaries().stream())
      .filter(secondary -> secondary.getFeedId().equals(agency.getId().getFeedId()))
      .findAny()
      .map(id -> timetableRepository.getSiteRepository().getRegularStop(id));
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
    return Optional.ofNullable(timetableRepository.getSiteRepository().getRegularStop(primaryId));
  }
}
