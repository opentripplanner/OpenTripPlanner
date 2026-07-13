package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.collection.TroveUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.InvalidOsmGeometry;
import org.opentripplanner.graph_builder.issues.TurnRestrictionBad;
import org.opentripplanner.graph_builder.issues.TurnRestrictionException;
import org.opentripplanner.graph_builder.issues.TurnRestrictionUnknown;
import org.opentripplanner.graph_builder.module.osm.TurnRestrictionTag.Direction;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmLevelFactory;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsmDatabase {

  private static final Logger LOG = LoggerFactory.getLogger(OsmDatabase.class);

  private final DataImportIssueStore issueStore;
  private final OsmLevelFactory osmLevelFactory;

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

  private final List<OsmWay> singleWayAreas = new ArrayList<>();

  private final Set<OsmEntity> processedAreas = new HashSet<>();

  /* Set of area way IDs */
  private final TLongSet areaWayIds = new TLongHashSet();

  /* Set of all node IDs of kept ways. Needed to mark which nodes to keep in stage 3. */
  private final TLongSet waysNodeIds = new TLongHashSet();

  /* Set of all node IDs of kept areas. Needed to mark which nodes to keep in stage 3. */
  private final TLongSet areaNodeIds = new TLongHashSet();

  /**
   * Track which vertical levels OSM entities belong to.
   * Level information can be set for ways, nodes and relations.
   * An entity only has an entry if at least one level is defined in OSM.
   * The ordering is important because it is used for building stairs and escalators.
   * The level is also used e.g. for building elevators and connecting areas.
   */
  private final ArrayListMultimap<OsmEntity, OsmLevel> entityLevels = ArrayListMultimap.create();

  /* Set of turn restrictions for each turn "from" way ID */
  private final Multimap<Long, TurnRestrictionTag> turnRestrictionsByFromWay =
    ArrayListMultimap.create();

  /* Set of turn restrictions for each turn "to" way ID */
  private final Multimap<Long, TurnRestrictionTag> turnRestrictionsByToWay =
    ArrayListMultimap.create();

  /*
   * Map of all transit stop nodes that lie within an area and which are connected to the area by
   * a relation. Keyed by the area's OSM way.
   */
  private final Multimap<OsmEntity, OsmNode> stopsInAreas = HashMultimap.create();

  /**
   * Set of all entrance nodes in stop areas, which will be treated as station entrances.
   */
  private final Set<OsmNode> entrancesInStopAreas = new HashSet<>();

  public OsmDatabase(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
    this.osmLevelFactory = new OsmLevelFactory(issueStore);
  }

  public OsmNode getNode(Long nodeId) {
    return nodesById.get(nodeId);
  }

  public OsmWay getWay(Long wayId) {
    return waysById.get(wayId);
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

  public boolean isEntranceInStopArea(OsmNode node) {
    return entrancesInStopAreas.contains(node);
  }

  /**
   * @return If a single level is defined for an entity return that level,
   * otherwise the default level is returned.
   */
  public OsmLevel findSingleLevelForEntity(OsmEntity entity) {
    List<OsmLevel> levels = entityLevels.get(entity);
    if (levels.size() == 1) {
      return levels.getFirst();
    } else {
      return OsmLevelFactory.DEFAULT;
    }
  }

  /**
   * @return All defined levels for an entity. If no levels are found a list with the default
   * level is returned.
   */
  public List<OsmLevel> getLevelsForEntity(OsmEntity entity) {
    if (entityLevels.containsKey(entity)) {
      return entityLevels.get(entity);
    } else {
      return List.of(OsmLevelFactory.DEFAULT);
    }
  }

  /**
   * @return A set of all defined levels for an entity. If no levels are found a set with the
   * default level is returned.
   */
  public Set<OsmLevel> getLevelSetForEntity(OsmEntity entity) {
    if (entityLevels.containsKey(entity)) {
      return Set.copyOf(entityLevels.get(entity));
    } else {
      return Set.of(OsmLevelFactory.DEFAULT);
    }
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
    createLevelsForEntity(node);
    if (node.isBikeParking()) {
      bikeParkingNodes.put(node.getId(), node);
    }
    if (node.isParkAndRide()) {
      carParkingNodes.put(node.getId(), node);
    }
    if (
      !(waysNodeIds.contains(node.getId()) ||
        areaNodeIds.contains(node.getId()) ||
        node.isBoardingLocation())
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
    if (!(way.isRelevantForRouting() || way.isBarrier())) {
      return;
    }

    createLevelsForEntity(way);

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
      (relation.isMultiPolygon() && (relation.isRoutable() || relation.isParkAndRide())) ||
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
      createLevelsForEntity(relation);
    } else if (
      !relation.isRestriction() &&
      !relation.isRoadRoute() &&
      !(relation.isMultiPolygon() && relation.isRoutable()) &&
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

    markNodesForKeeping(
      waysById.valueCollection().stream().filter(OsmWay::isRelevantForRouting).toList(),
      waysNodeIds
    );
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
  }

  private void createLevelsForEntity(OsmEntity entity) {
    entityLevels.putAll(entity, osmLevelFactory.createOsmLevelsForEntity(entity));
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
    AREA: for (OsmWay way : singleWayAreas) {
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
        // do not keep the way used in an area, it creates duplicated edges from the basic
        // street graph and from the area processing
        waysById.remove(way.getId());
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
    RELATION: for (OsmRelation relation : relationsById.valueCollection()) {
      if (processedAreas.contains(relation)) {
        continue;
      }
      if (
        !(relation.isMultiPolygon() &&
          (relation.isRoutable() || relation.isParkAndRide() || relation.isBikeParking()))
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
        }
      }
      processedAreas.add(relation);
      try {
        addArea(new OsmArea(relation, outerWays, innerWays, nodesById));
      } catch (OsmArea.AreaConstructionException | Ring.RingConstructionException e) {
        issueStore.add(new InvalidOsmGeometry(relation));
      }
    }
  }

  /**
   * Handler for a new OsmArea (single way area or multipolygon relations)
   */
  private void addArea(OsmArea area) {
    StreetTraversalPermission permissions = area.getPermission();
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
      } else if (relation.isRoute()) {
        processRoute(relation);
      } else if (relation.isPublicTransport()) {
        processPublicTransportStopArea(relation);
      }
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
          var updatedWay = way.copy().withTag(key, "yes").build();
          waysById.put(updatedWay.getId(), updatedWay);
        }
      });
  }

  /**
   * Store turn restrictions.
   */
  private void processRestriction(OsmRelation relation) {
    long from = -1;
    long to = -1;
    long via = -1;
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
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.NO_TURN,
        Direction.RIGHT,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "no_left_turn")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.NO_TURN,
        Direction.LEFT,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "no_straight_on")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.NO_TURN,
        Direction.STRAIGHT,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "no_u_turn")) {
      tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.U, relation.getId());
    } else if (relation.isTag("restriction", "only_straight_on")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.ONLY_TURN,
        Direction.STRAIGHT,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "only_right_turn")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.ONLY_TURN,
        Direction.RIGHT,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "only_left_turn")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.ONLY_TURN,
        Direction.LEFT,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "no_entry")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.NO_TURN,
        Direction.ENTRY,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "only_u_turn")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.ONLY_TURN,
        Direction.U,
        relation.getId()
      );
    } else if (relation.isTag("restriction", "no_exit")) {
      tag = new TurnRestrictionTag(
        via,
        TurnRestrictionType.NO_TURN,
        Direction.EXIT,
        relation.getId()
      );
    } else {
      issueStore.add(new TurnRestrictionUnknown(relation, relation.getTag("restriction")));
      return;
    }
    tag.modes = modes.clone();

    turnRestrictionsByFromWay.put(from, tag);
    turnRestrictionsByToWay.put(to, tag);
  }

  /**
   * Handle route=road and route=bicycle relations.
   */
  private void processRoute(OsmRelation relation) {
    if (relation.isBicycleRoute()) {
      // we treat networks without known network type like local networks
      var network = relation.getTagOpt("network").orElse("lcn");
      setNetworkForAllMembers(relation, network);
    }
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
          if (node != null) {
            if (node.isPlatformAccess()) {
              platformNodes.add(node);
            }
            if (node.isEntrance()) {
              entrancesInStopAreas.add(node);
            }
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
      Set<OsmLevel> areaLevelSet = getLevelSetForEntity(area);
      platformNodes
        .stream()
        .filter(node -> getLevelSetForEntity(node).containsAll(areaLevelSet))
        .forEach(node -> stopsInAreas.put(area, node));
    }
  }
}
