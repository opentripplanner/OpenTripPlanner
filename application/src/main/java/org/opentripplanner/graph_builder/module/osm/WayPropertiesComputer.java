package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.TCollections;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.osm.wayproperty.WayPropertiesPair;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes the {@link WayPropertiesPair} (traversal permissions and bicycle/walk safety) for
 * every way in an {@link OsmDatabase}.
 * <p>
 * Matching a way against the {@link org.opentripplanner.osm.wayproperty.WayPropertySet} is one of
 * the more expensive steps of graph building, so it is parallelized across all available CPU
 * cores.
 * <p>
 * Most ways in a graph share the same permissions and safety values (e.g. plain
 * {@code highway=residential} ways with no overrides), so the resulting {@link WayPropertiesPair}
 * instances are interned/deduplicated. The result is keyed by OSM way id in a primitive-long-keyed
 * map, which avoids the overhead of boxing millions of {@link Long} keys in a graph with a large
 * number of ways.
 */
class WayPropertiesComputer {

  private static final Logger LOG = LoggerFactory.getLogger(WayPropertiesComputer.class);

  private WayPropertiesComputer() {}

  /**
   * Compute the {@link WayPropertiesPair} for every way in {@code osmdb}, keyed by way id.
   */
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
