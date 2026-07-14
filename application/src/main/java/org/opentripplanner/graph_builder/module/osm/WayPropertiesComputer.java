package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.TCollections;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.osm.wayproperty.WayPropertiesPair;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Computes the [WayPropertiesPair] (traversal permissions and bicycle/walk safety) for every way
/// in an [OsmDatabase].
///
/// Matching a way against the [org.opentripplanner.osm.wayproperty.WayPropertySet] is one of the
/// more expensive steps of graph building, so it is parallelized across all available CPU cores.
///
/// Most ways in a graph share the same permissions and safety values (e.g. plain
/// `highway=residential` ways with no overrides), so the resulting [WayPropertiesPair] instances
/// are interned/deduplicated. The result is keyed by OSM way id in a primitive-long-keyed map,
/// which avoids the overhead of boxing millions of `Long` keys in a graph with a large number of
/// ways.
///
/// ### Memory consumption
///
/// The map is backed by Trove's open-addressing `TLongObjectHashMap`, sized upfront to the number
/// of ways at its default 0.5 load factor, so it never shrinks below that once built. For a graph
/// with 2 million ways that means a capacity of roughly 4 million slots, backed by three parallel
/// arrays:
///
/// | Array | Purpose | Size |
/// |---|---|---|
/// | `long[]` | way ids | ~30.5 MB |
/// | `Object[]` | value references (compressed oops) | ~15.3 MB |
/// | `byte[]` | slot state (FREE/FULL/REMOVED) | ~3.8 MB |
/// | **Total** | | **~50 MB** |
///
/// The deduplicated [WayPropertiesPair]/`WayProperties` payload itself is negligible (well under
/// 1 MB) since a typical way-property configuration only produces a few hundred to a few thousand
/// distinct combinations, regardless of how many ways reference them.
class WayPropertiesComputer {

  private static final Logger LOG = LoggerFactory.getLogger(WayPropertiesComputer.class);

  private WayPropertiesComputer() {}

  /// Compute the [WayPropertiesPair] for every way in `osmdb`, keyed by way id.
  static TLongObjectMap<WayPropertiesPair> compute(OsmDatabase osmdb) {
    var ways = osmdb.getWays();
    var progress = ProgressTracker.track("Compute way properties", 5_000, ways.size());
    LOG.info(progress.startMessage());

    var distinctPairs = new ConcurrentHashMap<WayPropertiesPair, WayPropertiesPair>();
    TLongObjectMap<WayPropertiesPair> props = new TLongObjectHashMap<>(ways.size());
    var synchronizedProps = TCollections.synchronizedMap(props);
    ways
      .parallelStream()
      .forEach(way -> {
        var pair = way.getOsmProvider().getWayPropertySet().getDataForWay(way);
        var canonicalPair = distinctPairs.computeIfAbsent(pair, p -> p);
        synchronizedProps.put(way.getId(), canonicalPair);
        //Keep lambda! A method-ref would log incorrect class and line number
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      });

    LOG.info(progress.completeMessage());
    return props;
  }
}
