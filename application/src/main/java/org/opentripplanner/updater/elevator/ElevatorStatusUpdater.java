package org.opentripplanner.updater.elevator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.UpdateManager;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepository;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepositorySnapshot;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;
import org.opentripplanner.street.elevator.ElevatorEdgeIndex;
import org.opentripplanner.transfer.regular.ClosedFacilitiesChanged;
import org.opentripplanner.updater.TransitRealTimeUpdateContext;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.spi.WriteDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polls the status of all elevators from a {@link DataSource} and applies it. Each poll is a full
 * snapshot: an elevator that is not in it is in service. If the snapshot has more than one status
 * for an elevator id, for example two elevators with the same id, the id is out of service if one
 * of them is.
 * <p>
 * The updater belongs to the transit write domain. Each poll submits two independent tasks:
 * <ul>
 *   <li>On the transit writer thread, the status is written to the {@link ElevatorStatusRepository}.
 *   If the elevators that are out of service changed, a {@link ClosedFacilitiesChanged} event with all
 *   of them is published in the same transaction.</li>
 *   <li>On the street writer thread, the elevators are set in or out of service on the street
 *   edges, so a street search does not use an elevator that is out of service.</li>
 * </ul>
 * The order does not matter: the transit data that depends on the elevators, the regular
 * transfers, is precomputed for every combination of closed elevators, and does not search the
 * street at runtime. The two tasks are not one transaction, so for a short time the street and
 * the transit data may disagree about an elevator, until both are committed.
 */
public class ElevatorStatusUpdater extends PollingGraphUpdater<TransitRealTimeUpdateContext> {

  private static final Logger LOG = LoggerFactory.getLogger(ElevatorStatusUpdater.class);

  private final DataSource<ElevatorStatus> source;
  private final ElevatorEdgeIndex elevatorEdgeIndex;
  private final UpdateManager streetUpdateManager;
  private final RepositoryHandle<
    ElevatorStatusRepositorySnapshot,
    ElevatorStatusRepository
  > elevatorStatusRepositoryHandle;

  /** The ids not in the street graph in the last update, only used on the street writer thread. */
  private Set<String> lastUnknownIds = Set.of();

  public ElevatorStatusUpdater(
    PollingGraphUpdaterParameters parameters,
    DataSource<ElevatorStatus> source,
    ElevatorEdgeIndex elevatorEdgeIndex,
    UpdateManager streetUpdateManager,
    RepositoryHandle<
      ElevatorStatusRepositorySnapshot,
      ElevatorStatusRepository
    > elevatorStatusRepositoryHandle
  ) {
    super(parameters);
    this.source = source;
    this.elevatorEdgeIndex = elevatorEdgeIndex;
    this.streetUpdateManager = streetUpdateManager;
    this.elevatorStatusRepositoryHandle = elevatorStatusRepositoryHandle;
    LOG.info(
      "Creating elevator status updater running every {}: {} elevators with an id in the graph",
      pollingPeriod(),
      elevatorEdgeIndex.elevatorIds().size()
    );
  }

  @Override
  public WriteDomain<TransitRealTimeUpdateContext> writeDomain() {
    return WriteDomain.TRANSIT;
  }

  @Override
  protected void runPolling() {
    if (source.update()) {
      var statuses = mergeDuplicates(source.getUpdates());
      updateGraph(context -> updateTransit(context, statuses));
      streetUpdateManager.submit(context -> updateStreet(statuses));
    }
  }

  private void updateStreet(List<ElevatorStatus> statuses) {
    var unknownIds = elevatorEdgeIndex.setOutOfService(outOfServiceIds(statuses));
    if (!unknownIds.equals(lastUnknownIds)) {
      if (!unknownIds.isEmpty()) {
        LOG.warn("Elevators out of service that are not in the street graph: {}", unknownIds);
      }
      lastUnknownIds = unknownIds;
    }
  }

  private void updateTransit(TransitRealTimeUpdateContext context, List<ElevatorStatus> statuses) {
    var repository = context.repository(elevatorStatusRepositoryHandle);
    var before = repository.outOfServiceIds();
    repository.setStatuses(statuses);
    var after = repository.outOfServiceIds();
    if (!after.equals(before)) {
      LOG.info("{} elevators are out of service", after.size());
      context.publish(new ClosedFacilitiesChanged(after));
    }
  }

  /** One status for each elevator id, out of service if any status of the id is. */
  static List<ElevatorStatus> mergeDuplicates(List<ElevatorStatus> statuses) {
    var statusById = new LinkedHashMap<String, ElevatorStatus>();
    for (var status : statuses) {
      statusById.merge(status.elevatorId(), status, (a, b) -> a.inService() ? b : a);
    }
    return List.copyOf(statusById.values());
  }

  private static Set<String> outOfServiceIds(List<ElevatorStatus> statuses) {
    return statuses
      .stream()
      .filter(status -> !status.inService())
      .map(ElevatorStatus::elevatorId)
      .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + source + "}";
  }
}
