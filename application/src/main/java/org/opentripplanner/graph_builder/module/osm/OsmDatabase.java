package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.collection.TroveUtils;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.graph_builder.issues.DisconnectedOsmNode;
import org.opentripplanner.graph_builder.issues.InvalidOsmGeometry;
import org.opentripplanner.graph_builder.issues.LevelAmbiguous;
import org.opentripplanner.graph_builder.issues.TurnRestrictionBad;
import org.opentripplanner.graph_builder.issues.TurnRestrictionException;
import org.opentripplanner.graph_builder.issues.TurnRestrictionUnknown;
import org.opentripplanner.graph_builder.module.osm.TurnRestrictionTag.Direction;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmLevel.Source;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;
import org.opentripplanner.osm.model.OsmTag;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.RepeatingTimePeriod;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsmDatabase {

  private static final Logger LOG = LoggerFactory.getLogger(OsmDatabase.class);

  private final DataImportIssueStore issueStore;

  /* Map of all nodes used in ways/areas keyed by their OSM ID */
  private final TLongObjectMap<OsmNode> nodesById = new TLongObjectHashMap<>();

  /* Map of all bike parking nodes, keyed by their OSM ID */
  private final TLongObjectMap<OsmNode> bikeParkingNodes = new TLongObjectHashMap<>();

  /* Map of all bike parking nodes, keyed by their OSM ID */
  private final TLongObjectMap<OsmNode> carParkingNodes = new TLongObjectHashMap<>();

  /* Map of all non-area ways keyed by their OSM ID */
  private final TLongObjectMap<OsmWay> waysById = new TLongObjectHashMap<>();

  /* Map of all area ways keyed by their OSM ID */
  private final TLongObjectMap<OsmWay> areaWaysById = new TLongObjectHashMap<>();

  /* Map of all relations keyed by their OSM ID */
  private final TLongObjectMap<OsmRelation> relationsById = new TLongObjectHashMap<>();

  /* All walkable areas */
  private final List<OsmArea> walkableAreas = new ArrayList<>();

  /* All P+R areas */
  private final List<OsmArea> parkAndRideAreas = new ArrayList<>();

  /* All bike parking areas */
  private final List<OsmArea> bikeParkingAreas = new ArrayList<>();

  /* Map of all area OSMWay for a given node */
  private final TLongObjectMap<Set<OsmWay>> areasForNode = new TLongObjectHashMap<>();

  /* Map of all area OSMWay for a given node */
  private final List<OsmWay> singleWayAreas = new ArrayList<>();

  private final Set<OsmEntity> processedAreas = new HashSet<>();

  /* Set of area way IDs */
  private final TLongSet areaWayIds = new TLongHashSet();

  /* Set of all node IDs of kept ways. Needed to mark which nodes to keep in stage 3. */
  private final TLongSet waysNodeIds = new TLongHashSet();

  /* Set of all node IDs of kept areas. Needed to mark which nodes to keep in stage 3. */
  private final TLongSet areaNodeIds = new TLongHashSet();

  /* Track which vertical level each OSM way belongs to, for building elevators etc. */
  private final Map<OsmEntity, OsmLevel> wayLevels = new HashMap<>();

  /* Set of turn restrictions for each turn "from" way ID */
  private final Multimap<Long, TurnRestrictionTag> turnRestrictionsByFromWay = ArrayListMultimap.create();

  /* Set of turn restrictions for each turn "to" way ID */
  private final Multimap<Long, TurnRestrictionTag> turnRestrictionsByToWay = ArrayListMultimap.create();

  /*
   * Map of all transit stop nodes that lie within an area and which are connected to the area by
   * a relation. Keyed by the area's OSM way.
   */
  private final Multimap<OsmEntity, OsmNode> stopsInAreas = HashMultimap.create();

  /*
   * ID of the next virtual node we create during building phase. Negative to prevent conflicts
   * with existing ones.
   */
  private long virtualNodeId = -100000;

  /**
   * If true, disallow zero floors and add 1 to non-negative numeric floors, as is generally done in
   * the United States. This does not affect floor names from level maps.
   */
  public boolean noZeroLevels = true;

  public OsmDatabase(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  public OsmNode getNode(Long nodeId) {
    return nodesById.get(nodeId);
  }

  public OsmWay getWay(Long nodeId) {
    return waysById.get(nodeId);
  }

  public Collection<OsmWay> getWays() {
    return Collections.unmodifiableCollection(waysById.valueCollection());
  }

  public boolean isAreaWay(Long wayId) {
    return areaWayIds.contains(wayId);
  }

  public int nodeCount() {
    return nodesById.size();
  }

  public int wayCount() {
    return waysById.size();
  }

  public Collection<OsmNode> getBikeParkingNodes() {
    return Collections.unmodifiableCollection(bikeParkingNodes.valueCollection());
  }

  public Collection<OsmNode> getCarParkingNodes() {
    return Collections.unmodifiableCollection(carParkingNodes.valueCollection());
  }

  public Collection<OsmArea> getWalkableAreas() {
    return Collections.unmodifiableCollection(walkableAreas);
  }

  public Collection<OsmArea> getParkAndRideAreas() {
    return Collections.unmodifiableCollection(parkAndRideAreas);
  }

  public Collection<OsmArea> getBikeParkingAreas() {
    return Collections.unmodifiableCollection(bikeParkingAreas);
  }

  public Collection<Long> getTurnRestrictionWayIds() {
    return Collections.unmodifiableCollection(turnRestrictionsByFromWay.keySet());
  }

  public Collection<TurnRestrictionTag> getFromWayTurnRestrictions(Long fromWayId) {
    return turnRestrictionsByFromWay.get(fromWayId);
  }

  public Collection<TurnRestrictionTag> getToWayTurnRestrictions(Long toWayId) {
    return turnRestrictionsByToWay.get(toWayId);
  }

  public Collection<OsmNode> getStopsInArea(OsmEntity areaParent) {
    return stopsInAreas.get(areaParent);
  }

  public OsmLevel getLevelForWay(OsmEntity way) {
    return Objects.requireNonNullElse(wayLevels.get(way), OsmLevel.DEFAULT);
  }

  public Set<OsmWay> getAreasForNode(Long nodeId) {
    Set<OsmWay> areas = areasForNode.get(nodeId);
    if (areas == null) {
      return Set.of();
    }
    return areas;
  }

  public boolean isNodeBelongsToWay(Long nodeId) {
    return waysNodeIds.contains(nodeId);
  }

  public void addNode(OsmNode node) {
    if (node.isBikeParking()) {
      bikeParkingNodes.put(node.getId(), node);
    }
    if (node.isParkAndRide()) {
      carParkingNodes.put(node.getId(), node);
    }
    if (
      !(
        waysNodeIds.contains(node.getId()) ||
        areaNodeIds.contains(node.getId()) ||
        node.isBoardingLocation()
      )
    ) {
      return;
    }

    if (nodesById.containsKey(node.getId())) {
      return;
    }
    nodesById.put(node.getId(), node);
  }

  public void addWay(OsmWay way) {
    /* only add ways once */
    long wayId = way.getId();
    if (waysById.containsKey(wayId) || areaWaysById.containsKey(wayId)) {
      return;
    }

    if (areaWayIds.contains(wayId)) {
      areaWaysById.put(wayId, way);
    }

    /* filter out ways that are not relevant for routing */
    if (
      !(way.isRoutable() || way.isParkAndRide() || way.isBikeParking() || way.isBoardingLocation())
    ) {
      return;
    }

    applyLevelsForWay(way);

    if (way.isRoutableArea()) {
      // this is an area that's a simple polygon. So we can just add it straight
      // to the areas, if it's not part of a relation.
      if (!areaWayIds.contains(wayId)) {
        singleWayAreas.add(way);
        areaWaysById.put(wayId, way);
        areaWayIds.add(wayId);
        way
          .getNodeRefs()
          .forEach(node -> {
            TroveUtils.addToMapSet(areasForNode, node, way);
            return true;
          });
      }
      return;
    }

    waysById.put(wayId, way);
  }

  public void addRelation(OsmRelation relation) {
    if (relationsById.containsKey(relation.getId())) {
      return;
    }

    if (
      relation.isMultiPolygon() &&
      (relation.isRoutable() || relation.isParkAndRide()) ||
      relation.isBikeParking()
    ) {
      // OSM MultiPolygons are ferociously complicated, and in fact cannot be processed
      // without reference to the ways that compose them. Accordingly, we will merely
      // mark the ways for preservation here, and deal with the details once we have
      // the ways loaded.
      if (!relation.isRoutable() && !relation.isParkAndRide() && !relation.isBikeParking()) {
        return;
      }
      for (OsmRelationMember member : relation.getMembers()) {
        areaWayIds.add(member.getRef());
      }
      applyLevelsForWay(relation);
    } else if (
      !relation.isRestriction() &&
      !relation.isRoadRoute() &&
      !(relation.isMultiPolygon() && relation.isRoutable()) &&
      !relation.isLevelMap() &&
      !relation.isStopArea() &&
      !(relation.isRoadRoute() || relation.isBicycleRoute())
    ) {
      return;
    }

    relationsById.put(relation.getId(), relation);
  }

  public void doneFirstPhaseRelations() {
    // nothing to do here
  }

  public void doneSecondPhaseWays() {
    // This copies relevant tags to the ways (highway=*) where it doesn't exist, so that
    // the way purging keeps the needed way around.
    // Multipolygons may be processed more than once, which may be needed since
    // some member might be in different files for the same multipolygon.

    // NOTE (AMB): this purging phase may not be necessary if highway tags are not
    // copied over from multipolygon relations. Perhaps we can get by with
    // only 2 steps -- ways+relations, followed by used nodes.
    // Ways can be tag-filtered in phase 1.

    markNodesForKeeping(waysById.valueCollection(), waysNodeIds);
    markNodesForKeeping(areaWaysById.valueCollection(), areaNodeIds);
  }

  public void doneThirdPhaseNodes() {
    processMultipolygonRelations();
    processSingleWayAreas();
  }

  /**
   * After all loading is done (from multiple OSM sources), post-process.
   */
  public void postLoad() {
    // handle turn restrictions, road names, and level maps in relations
    processRelations();

    // intersect non connected areas with ways
    processUnconnectedAreas();
  }

  /**
   * Check if a point is within an epsilon of a node.
   */
  private static boolean checkIntersectionDistance(Point p, OsmNode n, double epsilon) {
    return Math.abs(p.getY() - n.lat) < epsilon && Math.abs(p.getX() - n.lon) < epsilon;
  }

  /**
   * Check if two nodes are within an epsilon.
   */
  private static boolean checkDistanceWithin(OsmNode a, OsmNode b, double epsilon) {
    return Math.abs(a.lat - b.lat) < epsilon && Math.abs(a.lon - b.lon) < epsilon;
  }

  /**
   * Connect areas with ways when unconnected (areas outer rings crossing with ways at the same
   * level, but with no common nodes). Currently process P+R areas only, but could easily be
   * extended to others areas as well.
   */
  private void processUnconnectedAreas() {
    LOG.info("Intersecting unconnected areas...");

    /*
     * Create a spatial index for each segment of area outer rings. Note: The spatial index is
     * temporary and store only areas, so it should not take that much memory. Note 2: For
     * common nodes shared by different ways of different areas we only add them once, otherwise
     * we could end-up looping on creating new intersections.
     */
    Set<KeyPair> commonSegments = new HashSet<>();
    HashGridSpatialIndex<RingSegment> spndx = new HashGridSpatialIndex<>();
    for (OsmArea area : Iterables.concat(parkAndRideAreas, bikeParkingAreas)) {
      for (Ring ring : area.outermostRings) {
        processAreaRingForUnconnectedAreas(commonSegments, spndx, area, ring);
      }
    }

    // For each way, intersect with areas
    int nCreatedNodes = 0;
    for (OsmWay way : waysById.valueCollection()) {
      OsmLevel wayLevel = getLevelForWay(way);

      // For each segment of the way
      for (int i = 0; i < way.getNodeRefs().size() - 1; i++) {
        OsmNode nA = nodesById.get(way.getNodeRefs().get(i));
        OsmNode nB = nodesById.get(way.getNodeRefs().get(i + 1));
        if (nA == null || nB == null) {
          continue;
        }

        Envelope env = new Envelope(nA.lon, nB.lon, nA.lat, nB.lat);
        List<RingSegment> ringSegments = spndx.query(env);
        if (ringSegments.size() == 0) {
          continue;
        }
        LineString seg = GeometryUtils.makeLineString(nA.lon, nA.lat, nB.lon, nB.lat);

        for (RingSegment ringSegment : ringSegments) {
          boolean wayWasSplit = false;

          // Skip if both segments share a common node
          if (
            ringSegment.nA.getId() == nA.getId() ||
            ringSegment.nA.getId() == nB.getId() ||
            ringSegment.nB.getId() == nA.getId() ||
            ringSegment.nB.getId() == nB.getId()
          ) {
            continue;
          }

          // Skip if area and way are from "incompatible" levels
          OsmLevel areaLevel = getLevelForWay(ringSegment.area.parent);
          if (!wayLevel.equals(areaLevel)) {
            continue;
          }

          // Check for real intersection
          LineString seg2 = GeometryUtils.makeLineString(
            ringSegment.nA.lon,
            ringSegment.nA.lat,
            ringSegment.nB.lon,
            ringSegment.nB.lat
          );
          Geometry intersection = seg2.intersection(seg);
          Point p = null;
          if (intersection.isEmpty()) {
            continue;
          } else if (intersection instanceof Point) {
            p = (Point) intersection;
          } else {
            /*
             * This should never happen (intersection between two lines should be a
             * point or a multi-point).
             */
            LOG.error(
              "Alien intersection type between {} ({}--{}) and {} ({}--{}): {}",
              way,
              nA,
              nB,
              ringSegment.area.parent,
              ringSegment.nA,
              ringSegment.nB,
              intersection
            );
            continue;
          }

          // if the intersection is extremely close to one of the nodes of the road or the parking lot, just use that node
          // rather than splitting anything. See issue 1605.
          OsmNode splitNode;
          double epsilon = 0.0000001;

          // note that the if . . . else if structure of this means that if a node at one end of a (way|ring) segment is snapped,
          // the node at the other end cannot be, which is fine because the only time that could happen anyhow
          // would be if the nodes were duplicates.
          // prefer inserting into the ring segment to inserting into the way, so as to reduce graph complexity
          if (checkIntersectionDistance(p, nA, epsilon)) {
            // insert node A into the ring segment
            splitNode = nA;

            // This node is already a part of this ring (perhaps we inserted it
            // previously). No need to connect again. Note that this may not be a safe
            // assumption to make in all cases; suppose a way were to cross exactly
            // over a node *twice*, we would only add it the first time.
            if (ringSegment.ring.nodes.contains(splitNode)) {
              continue;
            }

            if (
              checkDistanceWithin(ringSegment.nA, nA, epsilon) ||
              checkDistanceWithin(ringSegment.nB, nA, epsilon)
            ) {
              issueStore.add(new DisconnectedOsmNode(nA, way, ringSegment.area.parent));
            }
          } else if (checkIntersectionDistance(p, nB, epsilon)) {
            // insert node B into the ring segment
            splitNode = nB;

            if (ringSegment.ring.nodes.contains(splitNode)) {
              continue;
            }

            if (
              checkDistanceWithin(ringSegment.nA, nB, epsilon) ||
              checkDistanceWithin(ringSegment.nB, nB, epsilon)
            ) {
              issueStore.add(new DisconnectedOsmNode(nB, way, ringSegment.area.parent));
            }
          } else if (checkIntersectionDistance(p, ringSegment.nA, epsilon)) {
            // insert node A into the road, if it's not already there

            // don't insert the same node twice. This is not always safe; suppose a way crosses over the same node in the parking area twice.
            // but we assume it doesn't (and even if it does, it's not a huge deal, as it is still connected elsewhere on the same way).
            if (way.getNodeRefs().contains(ringSegment.nA.getId())) continue;

            way.addNodeRef(ringSegment.nA.getId(), i + 1);

            if (
              checkDistanceWithin(ringSegment.nA, nA, epsilon) ||
              checkDistanceWithin(ringSegment.nA, nB, epsilon)
            ) {
              issueStore.add(new DisconnectedOsmNode(nB, ringSegment.area.parent, way));
            }
            // restart loop over way segments as we may have more intersections
            // as we haven't modified the ring, there is no need to modify the spatial index, so breaking here is fine
            i--;
            break;
          } else if (checkIntersectionDistance(p, ringSegment.nB, epsilon)) {
            // insert node B into the road, if it's not already there

            if (way.getNodeRefs().contains(ringSegment.nB.getId())) continue;

            way.addNodeRef(ringSegment.nB.getId(), i + 1);

            if (
              checkDistanceWithin(ringSegment.nB, nA, epsilon) ||
              checkDistanceWithin(ringSegment.nB, nB, epsilon)
            ) {
              issueStore.add(new DisconnectedOsmNode(ringSegment.nB, ringSegment.area.parent, way));
            }
            i--;
            break;
          } else {
            // create a node
            splitNode = createVirtualNode(p.getCoordinate());
            nCreatedNodes++;
            LOG.debug(
              "Adding virtual {}, intersection of {} ({}--{}) and area {} ({}--{}) at {}.",
              splitNode,
              way,
              nA,
              nB,
              ringSegment.area.parent,
              ringSegment.nA,
              ringSegment.nB,
              p
            );
            way.addNodeRef(splitNode.getId(), i + 1);

            /*
             * If we split the way, re-start the way segments loop as the newly created segments
             * could be intersecting again (in case one segment cut many others).
             */
            wayWasSplit = true;
          }

          /*
           * The line below is O(n^2) but we do not insert often and ring size should be
           * rather small.
           */
          int j = ringSegment.ring.nodes.indexOf(ringSegment.nB);
          ringSegment.ring.nodes.add(j, splitNode);

          /*
           * Update spatial index as we just split a ring segment. Note: we do not update
           * the first segment envelope, but as the new envelope is smaller than the
           * previous one this is harmless, apart from increasing a bit false positives
           * count.
           */
          RingSegment ringSegment2 = new RingSegment();
          ringSegment2.area = ringSegment.area;
          ringSegment2.ring = ringSegment.ring;
          ringSegment2.nA = splitNode;
          ringSegment2.nB = ringSegment.nB;
          Envelope env2 = new Envelope(
            ringSegment2.nA.lon,
            ringSegment2.nB.lon,
            ringSegment2.nA.lat,
            ringSegment2.nB.lat
          );
          spndx.insert(env2, ringSegment2);
          ringSegment.nB = splitNode;

          // if we split the way, backtrack over it again to check for additional splits
          // otherwise, we just continue the loop over ring segments
          if (wayWasSplit) {
            i--;
            break;
          }
        }
      }
    }
    LOG.info("Created {} virtual intersection nodes.", nCreatedNodes);
  }

  private void processAreaRingForUnconnectedAreas(
    Set<KeyPair> commonSegments,
    HashGridSpatialIndex<RingSegment> spndx,
    OsmArea area,
    Ring ring
  ) {
    for (int j = 0; j < ring.nodes.size(); j++) {
      RingSegment ringSegment = new RingSegment();
      ringSegment.area = area;
      ringSegment.ring = ring;
      ringSegment.nA = ring.nodes.get(j);
      ringSegment.nB = ring.nodes.get((j + 1) % ring.nodes.size());
      Envelope env = new Envelope(
        ringSegment.nA.lon,
        ringSegment.nB.lon,
        ringSegment.nA.lat,
        ringSegment.nB.lat
      );
      var key1 = new KeyPair(ringSegment.nA.getId(), ringSegment.nB.getId());
      var key2 = new KeyPair(ringSegment.nB.getId(), ringSegment.nA.getId());
      if (!commonSegments.contains(key1) && !commonSegments.contains(key2)) {
        spndx.insert(env, ringSegment);
        commonSegments.add(key1);
        commonSegments.add(key2);
      }
    }

    ring
      .getHoles()
      .forEach(hole -> processAreaRingForUnconnectedAreas(commonSegments, spndx, area, hole));
  }

  /**
   * Create a virtual OSM node, using a negative unique ID.
   *
   * @param c The location of the node to create.
   * @return The created node.
   */
  private OsmNode createVirtualNode(Coordinate c) {
    OsmNode node = new OsmNode();
    node.lon = c.x;
    node.lat = c.y;
    node.setId(virtualNodeId);
    virtualNodeId--;
    waysNodeIds.add(node.getId());
    nodesById.put(node.getId(), node);
    return node;
  }

  private void applyLevelsForWay(OsmEntity way) {
    /* Determine OSM level for each way, if it was not already set */
    if (!wayLevels.containsKey(way)) {
      // if this way is not a key in the wayLevels map, a level map was not
      // already applied in processRelations

      /* try to find a level name in tags */
      String levelName = null;
      OsmLevel level = OsmLevel.DEFAULT;
      if (way.hasTag("level")) { // TODO: floating-point levels &c.
        levelName = way.getTag("level");
        level =
          OsmLevel.fromString(levelName, OsmLevel.Source.LEVEL_TAG, noZeroLevels, issueStore, way);
      } else if (way.hasTag("layer")) {
        levelName = way.getTag("layer");
        level =
          OsmLevel.fromString(levelName, OsmLevel.Source.LAYER_TAG, noZeroLevels, issueStore, way);
      }
      if (level == null || (!level.reliable)) {
        issueStore.add(new LevelAmbiguous(levelName, way));
        level = OsmLevel.DEFAULT;
      }
      wayLevels.put(way, level);
    }
  }

  private void markNodesForKeeping(Collection<OsmWay> osmWays, TLongSet nodeSet) {
    for (OsmWay way : osmWays) {
      // Since the way is kept, update nodes-with-neighbors
      TLongList nodes = way.getNodeRefs();
      if (nodes.size() > 1) {
        nodeSet.addAll(nodes);
      }
    }
  }

  /**
   * Create areas from single ways.
   */
  private void processSingleWayAreas() {
    AREA:for (OsmWay way : singleWayAreas) {
      if (processedAreas.contains(way)) {
        continue;
      }
      TLongIterator longIterator = way.getNodeRefs().iterator();
      while (longIterator.hasNext()) {
        long nodeRef = longIterator.next();
        if (!nodesById.containsKey(nodeRef)) {
          continue AREA;
        }
      }
      try {
        addArea(new OsmArea(way, List.of(way), Collections.emptyList(), nodesById));
      } catch (OsmArea.AreaConstructionException | Ring.RingConstructionException e) {
        // this area cannot be constructed, but we already have all the
        // necessary nodes to construct it. So, something must be wrong with
        // the area; we'll mark it as processed so that we don't retry.
        issueStore.add(new InvalidOsmGeometry(way));
      } catch (IllegalArgumentException iae) {
        // This occurs when there are an invalid number of points in a LinearRing
        // Mark the ring as processed so we don't retry it.
        issueStore.add(new InvalidOsmGeometry(way));
      }
      processedAreas.add(way);
    }
  }

  /**
   * Copies useful metadata from multipolygon relations to the relevant ways, or to the area map.
   * This is done at a different time than processRelations(), so that way purging doesn't remove
   * the used ways.
   */
  private void processMultipolygonRelations() {
    RELATION:for (OsmRelation relation : relationsById.valueCollection()) {
      if (processedAreas.contains(relation)) {
        continue;
      }
      if (
        !(
          relation.isMultiPolygon() &&
          (relation.isRoutable() || relation.isParkAndRide() || relation.isBikeParking())
        )
      ) {
        continue;
      }
      // Area multipolygons -- pedestrian plazas
      ArrayList<OsmWay> innerWays = new ArrayList<>();
      ArrayList<OsmWay> outerWays = new ArrayList<>();
      for (OsmRelationMember member : relation.getMembers()) {
        OsmWay way = areaWaysById.get(member.getRef());
        if (way == null) {
          // relation includes way which does not exist in the data. Skip.
          continue RELATION;
        }
        TLongIterator wayNodeIterator = way.getNodeRefs().iterator();
        while (wayNodeIterator.hasNext()) {
          long nodeId = wayNodeIterator.next();
          if (nodesById.containsKey(nodeId)) {
            TroveUtils.addToMapSet(areasForNode, nodeId, way);
          } else {
            // this area is missing some nodes, perhaps because it is on
            // the edge of the region, so we will simply not route on it.
            continue RELATION;
          }
        }
        if (member.hasRoleInner()) {
          innerWays.add(way);
        } else if (member.hasRoleOuter()) {
          outerWays.add(way);
        } else {
          LOG.warn("Unexpected role '{}' in multipolygon", member.getRole());
        }
      }
      processedAreas.add(relation);
      try {
        addArea(new OsmArea(relation, outerWays, innerWays, nodesById));
      } catch (OsmArea.AreaConstructionException | Ring.RingConstructionException e) {
        issueStore.add(new InvalidOsmGeometry(relation));
        continue;
      }

      for (OsmRelationMember member : relation.getMembers()) {
        // multipolygons for attribute mapping
        if (!(member.hasTypeWay() && waysById.containsKey(member.getRef()))) {
          continue;
        }

        OsmEntity way = waysById.get(member.getRef());
        if (way == null) {
          continue;
        }
        String[] relationCopyTags = { "highway", "name", "ref" };
        for (String tag : relationCopyTags) {
          if (relation.hasTag(tag) && !way.hasTag(tag)) {
            way.addTag(tag, relation.getTag(tag));
          }
        }
        if (relation.isRailwayPlatform() && !way.hasTag("railway")) {
          way.addTag("railway", "platform");
        }
        if (relation.isPlatform() && !way.hasTag("public_transport")) {
          way.addTag("public_transport", "platform");
        }
      }
    }
  }

  /**
   * Handler for a new OsmArea (single way area or multipolygon relations)
   */
  private void addArea(OsmArea area) {
    StreetTraversalPermission permissions = area.parent
      .getOsmProvider()
      .getWayPropertySet()
      .getDataForWay(area.parent)
      .getPermission();
    if (area.parent.isRoutable() && permissions != StreetTraversalPermission.NONE) {
      walkableAreas.add(area);
    }
    // Please note: the same area can be both car P+R AND bike park.
    if (area.parent.isParkAndRide()) {
      parkAndRideAreas.add(area);
    }
    if (area.parent.isBikeParking()) {
      bikeParkingAreas.add(area);
    }
  }

  /**
   * Copies useful metadata from relations to the relevant ways/nodes.
   */
  private void processRelations() {
    LOG.debug("Processing relations...");

    for (OsmRelation relation : relationsById.valueCollection()) {
      if (relation.isRestriction()) {
        processRestriction(relation);
      } else if (relation.isLevelMap()) {
        processLevelMap(relation);
      } else if (relation.isRoute()) {
        processRoute(relation);
      } else if (relation.isPublicTransport()) {
        processPublicTransportStopArea(relation);
      }
    }
  }

  /**
   * Handle route=bicycle relations. Copies their network type to all way members.
   *
   * @see "https://wiki.openstreetmap.org/wiki/Tag:route%3Dbicycle"
   */
  private void processBicycleRoute(OsmRelation relation) {
    if (relation.isBicycleRoute()) {
      // we treat networks without known network type like local networks
      var network = relation.getTagOpt("network").orElse("lcn");
      setNetworkForAllMembers(relation, network);
    }
  }

  private void setNetworkForAllMembers(OsmRelation relation, String key) {
    relation
      .getMembers()
      .forEach(member -> {
        var isOsmWay = member.hasTypeWay();
        var way = waysById.get(member.getRef());
        // if it is an OSM way (rather than a node) and it doesn't already contain the tag
        // we add it
        if (way != null && isOsmWay && !way.hasTag(key)) {
          way.addTag(key, "yes");
        }
      });
  }

  /**
   * Store turn restrictions.
   */
  private void processRestriction(OsmRelation relation) {
    long from = -1, to = -1, via = -1;
    for (OsmRelationMember member : relation.getMembers()) {
      String role = member.getRole();
      if (role.equals("from")) {
        from = member.getRef();
      } else if (role.equals("to")) {
        to = member.getRef();
      } else if (role.equals("via")) {
        via = member.getRef();
      }
    }
    if (from == -1 || to == -1 || via == -1) {
      issueStore.add(
        new TurnRestrictionBad(relation.getId(), "One of from|via|to edges are empty in relation")
      );
      return;
    }

    TraverseModeSet modes = new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.CAR);
    String exceptModes = relation.getTag("except");
    if (exceptModes != null) {
      for (String m : exceptModes.split(";")) {
        if (m.equals("motorcar")) {
          modes.setCar(false);
        } else if (m.equals("bicycle")) {
          modes.setBicycle(false);
          issueStore.add(new TurnRestrictionException(via, from));
        }
      }
    }

    TurnRestrictionTag tag;
    if (relation.isTag("restriction", "no_right_turn")) {
      tag =
        new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.RIGHT, relation.getId());
    } else if (relation.isTag("restriction", "no_left_turn")) {
      tag =
        new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.LEFT, relation.getId());
    } else if (relation.isTag("restriction", "no_straight_on")) {
      tag =
        new TurnRestrictionTag(
          via,
          TurnRestrictionType.NO_TURN,
          Direction.STRAIGHT,
          relation.getId()
        );
    } else if (relation.isTag("restriction", "no_u_turn")) {
      tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.U, relation.getId());
    } else if (relation.isTag("restriction", "only_straight_on")) {
      tag =
        new TurnRestrictionTag(
          via,
          TurnRestrictionType.ONLY_TURN,
          Direction.STRAIGHT,
          relation.getId()
        );
    } else if (relation.isTag("restriction", "only_right_turn")) {
      tag =
        new TurnRestrictionTag(
          via,
          TurnRestrictionType.ONLY_TURN,
          Direction.RIGHT,
          relation.getId()
        );
    } else if (relation.isTag("restriction", "only_left_turn")) {
      tag =
        new TurnRestrictionTag(
          via,
          TurnRestrictionType.ONLY_TURN,
          Direction.LEFT,
          relation.getId()
        );
    } else if (relation.isTag("restriction", "only_u_turn")) {
      tag =
        new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.U, relation.getId());
    } else {
      issueStore.add(new TurnRestrictionUnknown(relation, relation.getTag("restriction")));
      return;
    }
    tag.modes = modes.clone();

    // set the time periods for this restriction, if applicable
    if (
      relation.hasTag("day_on") &&
      relation.hasTag("day_off") &&
      relation.hasTag("hour_on") &&
      relation.hasTag("hour_off")
    ) {
      try {
        tag.time =
          RepeatingTimePeriod.parseFromOsmTurnRestriction(
            relation.getTag("day_on"),
            relation.getTag("day_off"),
            relation.getTag("hour_on"),
            relation.getTag("hour_off"),
            relation.getOsmProvider()::getZoneId
          );
      } catch (NumberFormatException e) {
        LOG.info("Unparseable turn restriction: {}", relation.getId());
      }
    }

    turnRestrictionsByFromWay.put(from, tag);
    turnRestrictionsByToWay.put(to, tag);
  }

  /**
   * Process an OSM level map.
   */
  private void processLevelMap(OsmRelation relation) {
    var levelsTag = relation.getTag("levels");
    if (!StringUtils.hasValue(levelsTag)) {
      issueStore.add(
        Issue.issue(
          "InvalidLevelMap",
          "Could not parse level map for osm relation %d as it was malformed. Skipped.",
          relation.getId()
        )
      );
      return;
    }

    Map<String, OsmLevel> levels = OsmLevel.mapFromSpecList(
      levelsTag,
      Source.LEVEL_MAP,
      true,
      issueStore,
      relation
    );
    for (OsmRelationMember member : relation.getMembers()) {
      if (member.hasTypeWay() && waysById.containsKey(member.getRef())) {
        OsmWay way = waysById.get(member.getRef());
        if (way != null) {
          String role = member.getRole();
          // if the level map relation has a role:xyz tag, this way is something
          // more complicated than a single level (e.g. ramp/stairway).
          if (!relation.hasTag("role:" + role)) {
            if (levels.containsKey(role)) {
              wayLevels.put(way, levels.get(role));
            } else {
              LOG.warn("{} has undefined level {}", member.getRef(), role);
            }
          }
        }
      }
    }
  }

  /**
   * Handle route=road and route=bicycle relations.
   */
  private void processRoute(OsmRelation relation) {
    for (OsmRelationMember member : relation.getMembers()) {
      if (!(member.hasTypeWay() && waysById.containsKey(member.getRef()))) {
        continue;
      }

      OsmEntity way = waysById.get(member.getRef());
      if (way == null) {
        continue;
      }

      if (relation.hasTag("name")) {
        if (way.hasTag("otp:route_name")) {
          way.addTag(
            "otp:route_name",
            addUniqueName(way.getTag("otp:route_name"), relation.getTag("name"))
          );
        } else {
          way.addTag(new OsmTag("otp:route_name", relation.getTag("name")));
        }
      }
      if (relation.hasTag("ref")) {
        if (way.hasTag("otp:route_ref")) {
          way.addTag(
            "otp:route_ref",
            addUniqueName(way.getTag("otp:route_ref"), relation.getTag("ref"))
          );
        } else {
          way.addTag(new OsmTag("otp:route_ref", relation.getTag("ref")));
        }
      }
    }
    processBicycleRoute(relation);
  }

  /**
   * Process an OSM public transport stop area relation.
   * <p>
   * This goes through all public_transport=stop_area relations and adds the parent (either an area
   * or multipolygon relation) as the key and a Set of transit stop nodes that should be included in
   * the parent area as the value into stopsInAreas. This improves {@link org.opentripplanner.graph_builder.module.OsmBoardingLocationsModule}
   * by enabling us to have unconnected stop nodes within the areas by creating relations.
   *
   * @author hannesj
   * @see "http://wiki.openstreetmap.org/wiki/Tag:public_transport%3Dstop_area"
   */
  private void processPublicTransportStopArea(OsmRelation relation) {
    Set<OsmEntity> platformAreas = new HashSet<>();
    Set<OsmNode> platformNodes = new HashSet<>();
    for (OsmRelationMember member : relation.getMembers()) {
      switch (member.getType()) {
        case NODE -> {
          var node = nodesById.get(member.getRef());
          if (node != null && (node.isEntrance() || node.isBoardingLocation())) {
            platformNodes.add(node);
          }
        }
        case WAY -> {
          if (member.hasRolePlatform() && areaWaysById.containsKey(member.getRef())) {
            platformAreas.add(areaWaysById.get(member.getRef()));
          }
        }
        case RELATION -> {
          if (member.hasRolePlatform() && relationsById.containsKey(member.getRef())) {
            platformAreas.add(relationsById.get(member.getRef()));
          }
        }
      }
    }

    for (OsmEntity area : platformAreas) {
      if (area == null) {
        throw new RuntimeException(
          "Could not process public transport relation '%s' (%s)".formatted(
              relation,
              relation.url()
            )
        );
      }
      // single platform area presumably contains only one level in most cases
      // a node inside it may specify several levels if it is an elevator
      // make sure each node has access to the current platform level
      final Set<String> filterLevels = area.getLevels();
      platformNodes
        .stream()
        .filter(node -> node.getLevels().containsAll(filterLevels))
        .forEach(node -> stopsInAreas.put(area, node));
    }
  }

  private String addUniqueName(String routes, String name) {
    String[] names = routes.split(", ");
    for (String existing : names) {
      if (existing.equals(name)) {
        return routes;
      }
    }
    return routes + ", " + name;
  }

  // Simple holder for the spatial index
  static class RingSegment {

    OsmArea area;

    Ring ring;

    OsmNode nA;

    OsmNode nB;
  }

  private record KeyPair(long id0, long id1) {}
}
