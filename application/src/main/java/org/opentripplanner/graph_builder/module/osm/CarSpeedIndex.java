package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.osm.model.TraverseDirection.BACKWARD;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;

import gnu.trove.impl.Constants;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import java.util.Collection;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Indexes the forward and backward car speed of every way in a collection, keyed by OSM way id.
///
/// A car speed only depends on the way and the direction, but matching a way against the speed
/// pickers of the [org.opentripplanner.osm.wayproperty.WayPropertySet] is expensive. Computing it
/// once per way and direction, in parallel across all available CPU cores, is much faster than
/// computing it for every edge of the way.
class CarSpeedIndex {

  private static final Logger LOG = LoggerFactory.getLogger(CarSpeedIndex.class);

  private static final float NO_VALUE = -989;

  private final TLongFloatMap forward;
  private final TLongFloatMap backward;

  private CarSpeedIndex(TLongFloatMap forward, TLongFloatMap backward) {
    this.forward = forward;
    this.backward = backward;
  }

  /// Build the index for every way in `ways`, reporting speed issues to `issueStore`.
  static CarSpeedIndex of(Collection<OsmWay> ways, DataImportIssueStore issueStore) {
    var progress = ProgressTracker.track("Compute car speeds", 5_000, ways.size());
    LOG.info(progress.startMessage());

    // Trove maps aren't thread-safe: compute in parallel, put one at a time via forEachOrdered.
    var speeds = ways.parallelStream().map(way -> {
      var tagMapper = way.getOsmProvider().getOsmTagMapper();
      var waySpeeds = new WaySpeeds(
        way.getId(),
        tagMapper.getCarSpeedForWay(way, FORWARD, issueStore),
        tagMapper.getCarSpeedForWay(way, BACKWARD, issueStore)
      );
      //Keep lambda! A method-ref would log incorrect class and line number
      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
      return waySpeeds;
    });

    TLongFloatMap forward = newMap(ways.size());
    TLongFloatMap backward = newMap(ways.size());

    speeds.forEachOrdered(waySpeeds -> {
      forward.put(waySpeeds.wayId(), waySpeeds.forward());
      backward.put(waySpeeds.wayId(), waySpeeds.backward());
    });

    LOG.info(progress.completeMessage());
    return new CarSpeedIndex(forward, backward);
  }

  /// The forward and backward car speed of a single way.
  private record WaySpeeds(long wayId, float forward, float backward) {}

  private static TLongFloatMap newMap(int capacity) {
    return new TLongFloatHashMap(
      capacity,
      Constants.DEFAULT_LOAD_FACTOR,
      Constants.DEFAULT_LONG_NO_ENTRY_VALUE,
      NO_VALUE
    );
  }

  /// Return the car speed in the forward direction of the way with the given id.
  float forward(long wayId) {
    return requireValue(forward.get(wayId), wayId);
  }

  /// Return the car speed in the backward direction of the way with the given id.
  float backward(long wayId) {
    return requireValue(backward.get(wayId), wayId);
  }

  private static float requireValue(float speed, long wayId) {
    if (speed == NO_VALUE) {
      throw new IllegalArgumentException("No speed for way %s".formatted(wayId));
    }
    return speed;
  }
}
