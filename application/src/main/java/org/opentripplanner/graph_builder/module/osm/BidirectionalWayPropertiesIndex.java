package org.opentripplanner.graph_builder.module.osm;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.BidirectionalWayProperties;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Indexes the [BidirectionalWayProperties] (traversal permissions and bicycle/walk safety) of
/// every way in a collection, keyed by OSM way id.
///
/// Matching a way against the [org.opentripplanner.osm.wayproperty.WayPropertySet] is one of the
/// more expensive steps of graph building, so the index is built in parallel across all available
/// CPU cores.
///
/// Most ways in a graph share the same permissions and safety values (e.g. plain
/// `highway=residential` ways with no overrides), so the resulting
/// [BidirectionalWayProperties] instances are interned/deduplicated. The index is backed by a
/// primitive-long-keyed map, which avoids the overhead of boxing millions of `Long` keys in a graph
/// with a large number of ways.
///
/// ### Memory consumption
///
/// The map is backed by HPPC's open-addressing `LongObjectHashMap`, sized upfront to the number of
/// ways at its default 0.75 load factor, so it never shrinks below that once built. For a graph with
/// 2 million ways that means a capacity of roughly 4 million slots (rounded up to a power of two),
/// backed by two parallel arrays:
///
/// | Array | Purpose | Size |
/// |---|---|---|
/// | `long[]` | way ids | ~32 MB |
/// | `Object[]` | value references (compressed oops) | ~16 MB |
/// | **Total** | | **~48 MB** |
///
/// The deduplicated [BidirectionalWayProperties]/`WayProperties` payload itself is negligible (well
/// under 1 MB) since a typical way-property configuration only produces a few hundred to a few
/// thousand distinct combinations, regardless of how many ways reference them.
class BidirectionalWayPropertiesIndex {

  private static final Logger LOG = LoggerFactory.getLogger(BidirectionalWayPropertiesIndex.class);

  private final LongObjectMap<BidirectionalWayProperties> index;

  private BidirectionalWayPropertiesIndex(LongObjectMap<BidirectionalWayProperties> index) {
    this.index = index;
  }

  /// Build the index for every way in `ways`.
  static BidirectionalWayPropertiesIndex of(Collection<OsmWay> ways) {
    var progress = ProgressTracker.track("Compute way properties", 5_000, ways.size());
    LOG.info(progress.startMessage());

    var distinctProps = new ConcurrentHashMap<
      BidirectionalWayProperties,
      BidirectionalWayProperties
    >();
    LongObjectHashMap<BidirectionalWayProperties> index = new LongObjectHashMap<>(ways.size());
    ways.parallelStream().forEach(way -> {
      var props = way.getOsmProvider().getWayPropertySet().getDataForWay(way);
      var canonicalProps = distinctProps.computeIfAbsent(props, p -> p);
      // HPPC has no synchronized-wrapper, so the concurrent writes below are
      // guarded by a manual monitor on the map itself instead.
      synchronized (index) {
        index.put(way.getId(), canonicalProps);
      }
      // Keep lambda! A method-ref would log incorrect class and line number
      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    });

    LOG.info(progress.completeMessage());
    return new BidirectionalWayPropertiesIndex(index);
  }

  /// Return the [BidirectionalWayProperties] for the way with the given id.
  BidirectionalWayProperties forWay(long wayId) {
    return index.get(wayId);
  }
}
