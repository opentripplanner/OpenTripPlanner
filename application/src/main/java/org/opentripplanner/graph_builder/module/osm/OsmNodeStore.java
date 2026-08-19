package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.map.TLongByteMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongByteHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;

/**
 * A memory-efficient store for the {@link OsmNode}s used in ways/areas, keyed by their OSM ID.
 * <p>
 * This is the single largest in-memory structure during OSM processing, so instead of a
 * {@code TLongObjectMap<OsmNode>} (which needs a full {@link OsmNode} object, including a nested
 * tags map, per node) node data is split into three parallel, node ID-keyed stores. {@link
 * #get(long)} reassembles an {@link OsmNode} on demand from these.
 * <p>
 * Because nodes are reassembled on every {@link #get(long)} call, two {@link OsmNode} instances
 * returned for the same ID are generally <b>not</b> the same object. Callers must rely on {@link
 * OsmEntity#equals} (which compares by ID and type) rather than reference equality.
 */
class OsmNodeStore {

  /** OSM coordinates have 7 decimal digits of precision, i.e. about 1cm - the same as this fixed point encoding. */
  private static final double FIXED_POINT_FACTOR = 1e7;

  /** Coordinates of all retained nodes, packed as two fixed-point ints in one long. */
  private final TLongLongMap coordinatesById = new TLongLongHashMap();

  /**
   * Index into {@link #providers} of the provider of all retained nodes, minus one entry: nodes
   * from the first (index 0) provider are left out, relying on {@link TLongByteMap#get} returning
   * its no-entry-value of {@code 0} for them. In the very common case of there being only a single
   * provider - a single input OSM file - this map therefore stays completely empty.
   */
  private final TLongByteMap nonDefaultProviderIndexById = new TLongByteHashMap();
  private final List<OsmProvider> providers = new ArrayList<>();

  /**
   * Tags of the retained nodes. The vast majority of nodes have no tags at all, so this map only
   * has an entry for nodes that do, unlike the coordinate and provider stores above.
   */
  private final TLongObjectMap<Map<String, String>> tagsById = new TLongObjectHashMap<>();

  /**
   * Stores the given node, unless a node with the same id is already present.
   */
  void add(OsmNode node) {
    long id = node.getId();
    if (coordinatesById.containsKey(id)) {
      return;
    }
    coordinatesById.put(id, packCoordinate(node.lat, node.lon));
    byte providerIndex = providerIndex(node.getOsmProvider());
    if (providerIndex != 0) {
      nonDefaultProviderIndexById.put(id, providerIndex);
    }
    var tags = node.getTags();
    if (!tags.isEmpty()) {
      tagsById.put(id, tags);
    }
  }

  /**
   * Reassembles and returns the node with the given id, or {@code null} if it isn't stored.
   */
  OsmNode get(long id) {
    if (!coordinatesById.containsKey(id)) {
      return null;
    }
    long packedCoordinate = coordinatesById.get(id);
    var provider = providers.get(nonDefaultProviderIndexById.get(id));
    var builder = OsmNode.of()
      .withId(id)
      .withLatLon(unpackLat(packedCoordinate), unpackLon(packedCoordinate));
    // OsmNodeBuilder.withOsmProvider() rejects null, but some (mostly test) nodes are built
    // without ever setting a provider, so only call it when there actually is one.
    if (provider != null) {
      builder.withOsmProvider(provider);
    }
    var tags = tagsById.get(id);
    if (tags != null) {
      builder.withTags(tags);
    }
    return builder.build();
  }

  boolean contains(long id) {
    return coordinatesById.containsKey(id);
  }

  /**
   * Returns just the coordinate of the node with the given id, or {@code null} if it isn't
   * stored. Cheaper than {@link #get(long)} for the (common) call sites that don't need the
   * node's tags or provider, since it skips reassembling a full {@link OsmNode}.
   */
  Coordinate getCoordinate(long id) {
    if (!coordinatesById.containsKey(id)) {
      return null;
    }
    long packedCoordinate = coordinatesById.get(id);
    return new Coordinate(unpackLon(packedCoordinate), unpackLat(packedCoordinate));
  }

  int size() {
    return coordinatesById.size();
  }

  /**
   * Returns the index of the given provider in {@link #providers}, adding it if it isn't already
   * known. There are normally only a handful of distinct providers (one per input OSM file), so a
   * byte is more than enough and a linear scan is cheap.
   */
  private byte providerIndex(OsmProvider provider) {
    for (int i = 0; i < providers.size(); i++) {
      if (providers.get(i) == provider) {
        return (byte) i;
      }
    }
    if (providers.size() >= Byte.MAX_VALUE) {
      throw new IllegalStateException("Too many distinct OSM providers, cannot index as a byte");
    }
    providers.add(provider);
    return (byte) (providers.size() - 1);
  }

  private static long packCoordinate(double lat, double lon) {
    int latFixed = (int) Math.round(lat * FIXED_POINT_FACTOR);
    int lonFixed = (int) Math.round(lon * FIXED_POINT_FACTOR);
    return (((long) latFixed) << 32) | (lonFixed & 0xFFFFFFFFL);
  }

  private static double unpackLat(long packedCoordinate) {
    return ((int) (packedCoordinate >> 32)) / FIXED_POINT_FACTOR;
  }

  private static double unpackLon(long packedCoordinate) {
    return ((int) packedCoordinate) / FIXED_POINT_FACTOR;
  }
}
