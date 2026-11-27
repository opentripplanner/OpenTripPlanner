package org.opentripplanner.updater.elevator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.street.elevator.ElevatorEdgeIndex;
import org.opentripplanner.updater.TransitRealTimeUpdateContext;
import org.opentripplanner.updater.elevator.model.ElevatorStatus;
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
 * The updater belongs to the transit write domain. Each poll is applied in one task on the
 * transit writer thread, in this order:
 * <ol>
 *   <li>The elevators are set in or out of service on the street edges, see
 *   {@link ElevatorEdgeIndex#setOutOfService}. Street searches see the change at once.</li>
 *   <li>An {@link ElevatorStatusChanged} event with all the elevators out of service is published.
 *   The transit data that depends on it, the regular transfers, changes when the transaction
 *   commits.</li>
 * </ol>
 * The order matters when an elevator comes back into service: the regular transfers re-cost their
 * paths over the street edges, so the elevator must be in service on the street before the
 * transfers through it are used again. When an elevator goes out of service, the transfers through
 * it are replaced or dropped at the commit.
 * <p>
 * The task does nothing if the elevators out of service are the ones of the last event it
 * published. The event is only remembered when it was handled, so if handling it fails, the next
 * poll tries again.
 */
public class ElevatorStatusUpdater extends PollingGraphUpdater<TransitRealTimeUpdateContext> {

  private static final Logger LOG = LoggerFactory.getLogger(ElevatorStatusUpdater.class);

  private final DataSource<ElevatorStatus> source;
  private final ElevatorEdgeIndex elevatorEdgeIndex;

  /** The ids not in the street graph in the last update. Only used on the transit writer thread. */
  private Set<String> lastUnknownIds = Set.of();

  /**
   * The elevators out of service in the last published event. Every elevator is in service before
   * the first event. Only used on the transit writer thread.
   */
  private Set<String> lastPublishedOutOfServiceIds = Set.of();

  public ElevatorStatusUpdater(
    PollingGraphUpdaterParameters parameters,
    DataSource<ElevatorStatus> source,
    ElevatorEdgeIndex elevatorEdgeIndex
  ) {
    super(parameters);
    this.source = source;
    this.elevatorEdgeIndex = elevatorEdgeIndex;
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
      var outOfServiceIds = outOfServiceIds(source.getUpdates());
      updateGraph(context -> update(context, outOfServiceIds));
    }
  }

  private void update(TransitRealTimeUpdateContext context, Set<String> outOfServiceIds) {
    if (outOfServiceIds.equals(lastPublishedOutOfServiceIds)) {
      return;
    }
    // The street edges first, so a reopened elevator is in service before the transfers use it
    updateStreetEdges(outOfServiceIds);
    LOG.info("{} elevators are out of service", outOfServiceIds.size());
    context.publish(new ElevatorStatusChanged(outOfServiceIds));
    // Only remembered when the event was handled, so a failure is retried on the next poll
    lastPublishedOutOfServiceIds = outOfServiceIds;
  }

  private void updateStreetEdges(Set<String> outOfServiceIds) {
    var unknownIds = elevatorEdgeIndex.setOutOfService(outOfServiceIds);
    if (!unknownIds.equals(lastUnknownIds)) {
      if (!unknownIds.isEmpty()) {
        LOG.warn("Elevators out of service that are not in the street graph: {}", unknownIds);
      }
      lastUnknownIds = unknownIds;
    }
  }

  /** The ids of the elevators out of service, an id is out of service if any of its statuses is. */
  static Set<String> outOfServiceIds(List<ElevatorStatus> statuses) {
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
