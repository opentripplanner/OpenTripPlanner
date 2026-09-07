package org.opentripplanner.updater.spi;

import org.opentripplanner.updater.AlertRealTimeUpdateContext;
import org.opentripplanner.updater.StreetRealTimeUpdateContext;
import org.opentripplanner.updater.TransitRealTimeUpdateContext;

/**
 * A write domain owns a disjoint set of mutable data. Real-time updates are applied to one domain
 * at a time, so a write task may only modify data owned by its own domain.
 *
 * @param <C> the update context handed to this domain's write tasks
 */
public final class WriteDomain<C> {

  /**
   * Timetable data and realtime vehicles.
   */
  public static final WriteDomain<TransitRealTimeUpdateContext> TRANSIT = new WriteDomain<>(
    "TRANSIT"
  );

  /**
   * The transit alerts. They have a write domain of their own so that every alert update task can
   * be committed - or rolled back - on its own, without forcing the timetable to publish a new
   * snapshot at the same rate.
   */
  public static final WriteDomain<AlertRealTimeUpdateContext> ALERT = new WriteDomain<>("ALERT");

  /**
   * The street graph and the vehicle-rental and vehicle-parking repositories.
   */
  public static final WriteDomain<StreetRealTimeUpdateContext> STREET = new WriteDomain<>("STREET");

  private final String name;

  private WriteDomain(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
