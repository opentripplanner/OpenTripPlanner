package org.opentripplanner.updater.elevator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusChanged;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepository;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;
import org.opentripplanner.street.elevator.ElevatorEdgeIndex;
import org.opentripplanner.updater.StreetRealTimeUpdateContext;
import org.opentripplanner.updater.TransitRealTimeUpdateContext;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.spi.WriteDomain;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
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
 *   <li>On the transit writer thread, the status is written to the
 *   {@link ElevatorStatusRepository}. If the elevators that are out of service changed, an
 *   {@link ElevatorStatusChanged} event with all of them is published in the same
 *   transaction.</li>
 *   <li>On the street writer thread, the elevators are set in or out of service on the street
 *   edges, so a street search does not use an elevator that is out of service.</li>
 * </ul>
 * The order does not matter: the transit data that depends on the elevators, the regular
 * transfers, has alternative paths precomputed that avoid the elevators of each best path, and
 * does not search the street at runtime. The two tasks are not one transaction, so for a short time the street and
 * the transit data may disagree about an elevator, until both are committed.
 * <p>
 * The transit task does nothing if the statuses are the ones it last wrote. It compares the
 * elevators out of service with the ones of the last event it published, not with the
 * repository, so if writing the status or handling the event fails, the next poll tries again.
 */
public class ElevatorStatusUpdater extends PollingGraphUpdater<TransitRealTimeUpdateContext> {

  private static final Logger LOG = LoggerFactory.getLogger(ElevatorStatusUpdater.class);

  private final DataSource<ElevatorStatus> source;
  private final ElevatorEdgeIndex elevatorEdgeIndex;
  private final WriteToGraphCallback<StreetRealTimeUpdateContext> streetWriter;

  /** The ids not in the street graph in the last update, only used on the street writer thread. */
  private Set<String> lastUnknownIds = Set.of();

  /**
   * The statuses last written to the repository, {@code null} before the first write. Only used
   * on the transit writer thread.
   */
  @Nullable
  private Set<ElevatorStatus> lastWrittenStatuses = null;

  /**
   * The elevators out of service in the last published event. Every elevator is in service before
   * the first event. Only used on the transit writer thread.
   */
  private Set<String> lastPublishedOutOfServiceIds = Set.of();

  public ElevatorStatusUpdater(
    PollingGraphUpdaterParameters parameters,
    DataSource<ElevatorStatus> source,
    ElevatorEdgeIndex elevatorEdgeIndex,
    WriteToGraphCallback<StreetRealTimeUpdateContext> streetWriter
  ) {
    super(parameters);
    this.source = source;
    this.elevatorEdgeIndex = elevatorEdgeIndex;
    this.streetWriter = streetWriter;
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
      streetWriter.execute(context -> updateStreet(statuses));
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
    var statusSet = Set.copyOf(statuses);
    if (statusSet.equals(lastWrittenStatuses)) {
      return;
    }
    // The status is written first, so the handlers of the event see it
    context.elevatorStatusRepository().setStatuses(statuses);
    var outOfServiceIds = outOfServiceIds(statuses);
    if (!outOfServiceIds.equals(lastPublishedOutOfServiceIds)) {
      LOG.info("{} elevators are out of service", outOfServiceIds.size());
      context.publish(new ElevatorStatusChanged(outOfServiceIds));
    }
    // Only remembered when both succeeded, so a failure is retried on the next poll
    lastPublishedOutOfServiceIds = outOfServiceIds;
    lastWrittenStatuses = statusSet;
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
