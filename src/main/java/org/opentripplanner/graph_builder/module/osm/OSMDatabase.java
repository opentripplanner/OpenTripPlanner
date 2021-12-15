package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.RepeatingTimePeriod;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.LevelAmbiguous;
import org.opentripplanner.graph_builder.issues.PublicTransportRelationSkipped;
import org.opentripplanner.graph_builder.issues.TooManyAreasInRelation;
import org.opentripplanner.graph_builder.issues.TurnRestrictionBad;
import org.opentripplanner.graph_builder.issues.TurnRestrictionException;
import org.opentripplanner.graph_builder.issues.TurnRestrictionUnknown;
import org.opentripplanner.graph_builder.module.osm.TurnRestrictionTag.Direction;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMLevel.Source;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OSMDatabase {

    private static Logger LOG = LoggerFactory.getLogger(OSMDatabase.class);

    private DataImportIssueStore issueStore;

    /* Map of all nodes used in ways/areas keyed by their OSM ID */
    private TLongObjectMap<OSMNode> nodesById = new TLongObjectHashMap<>();

    /* Map of all bike parking nodes, keyed by their OSM ID */
    private TLongObjectMap<OSMNode> bikeParkingNodes = new TLongObjectHashMap<>();

    /* Map of all bike parking nodes, keyed by their OSM ID */
    private TLongObjectMap<OSMNode> carParkingNodes = new TLongObjectHashMap<>();

    /* Map of all non-area ways keyed by their OSM ID */
    private TLongObjectMap<OSMWay> waysById = new TLongObjectHashMap<>();

    /* Map of all area ways keyed by their OSM ID */
    private TLongObjectMap<OSMWay> areaWaysById = new TLongObjectHashMap<>();

    /* Map of all relations keyed by their OSM ID */
    private TLongObjectMap<OSMRelation> relationsById = new TLongObjectHashMap<>();

    /* All walkable areas */
    private List<Area> walkableAreas = new ArrayList<Area>();

    /* All P+R areas */
    private List<Area> parkAndRideAreas = new ArrayList<Area>();

    /* All bike parking areas */
    private List<Area> bikeParkingAreas = new ArrayList<Area>();

    /* Map of all area OSMWay for a given node */
    private TLongObjectMap<Set<OSMWay>> areasForNode = new TLongObjectHashMap<>();

    /* Map of all area OSMWay for a given node */
    private List<OSMWay> singleWayAreas = new ArrayList<OSMWay>();

    private Set<OSMWithTags> processedAreas = new HashSet<OSMWithTags>();

    /* Set of area way IDs */
    private TLongSet areaWayIds = new TLongHashSet();

    /* Set of all node IDs of kept ways. Needed to mark which nodes to keep in stage 3. */
    private TLongSet waysNodeIds = new TLongHashSet();

    /* Set of all node IDs of kept areas. Needed to mark which nodes to keep in stage 3. */
    private TLongSet areaNodeIds = new TLongHashSet();

    /* Track which vertical level each OSM way belongs to, for building elevators etc. */
    private Map<OSMWithTags, OSMLevel> wayLevels = new HashMap<OSMWithTags, OSMLevel>();

    /* Set of turn restrictions for each turn "from" way ID */
    private Multimap<Long, TurnRestrictionTag> turnRestrictionsByFromWay = ArrayListMultimap
            .create();

    /* Set of turn restrictions for each turn "to" way ID */
    private Multimap<Long, TurnRestrictionTag> turnRestrictionsByToWay = ArrayListMultimap.create();

    /*
     * Map of all transit stop nodes that lie within an area and which are connected to the area by
     * a relation. Keyed by the area's OSM way.
     */
    private Map<OSMWithTags, Set<OSMNode>> stopsInAreas = new HashMap<OSMWithTags, Set<OSMNode>>();

    /*
     * ID of the next virtual node we create during building phase. Negative to prevent conflicts
     * with existing ones.
     */
    private long virtualNodeId = -100000;

    /**
     * If true, disallow zero floors and add 1 to non-negative numeric floors, as is generally done
     * in the United States. This does not affect floor names from level maps.
     */
    public boolean noZeroLevels = true;

    public OSMDatabase(DataImportIssueStore issueStore) {
        this.issueStore = issueStore;
    }

    public OSMNode getNode(Long nodeId) {
        return nodesById.get(nodeId);
    }

    public OSMWay getWay(Long nodeId) {
        return waysById.get(nodeId);
    }

    public Collection<OSMWay> getWays() {
        return Collections.unmodifiableCollection(waysById.valueCollection());
    }

    public int nodeCount() {
        return nodesById.size();
    }

    public int wayCount() {
        return waysById.size();
    }

    public Collection<OSMNode> getBikeParkingNodes() {
        return Collections.unmodifiableCollection(bikeParkingNodes.valueCollection());
    }

    public Collection<OSMNode> getCarParkingNodes() {
        return Collections.unmodifiableCollection(carParkingNodes.valueCollection());
    }

    public Collection<Area> getWalkableAreas() {
        return Collections.unmodifiableCollection(walkableAreas);
    }

    public Collection<Area> getParkAndRideAreas() {
        return Collections.unmodifiableCollection(parkAndRideAreas);
    }

    public Collection<Area> getBikeParkingAreas() {
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

    public Collection<OSMNode> getStopsInArea(OSMWithTags areaParent) {
        return stopsInAreas.get(areaParent);
    }

    public OSMLevel getLevelForWay(OSMWithTags way) {
        OSMLevel level = wayLevels.get(way);
        return level != null ? level : OSMLevel.DEFAULT;
    }

    public Set<OSMWay> getAreasForNode(Long nodeId) {
        Set<OSMWay> areas = areasForNode.get(nodeId);
        if (areas == null) {
            return Set.of();
        }
        return areas;
    }

    public boolean isNodeBelongsToWay(Long nodeId) {
        return waysNodeIds.contains(nodeId);
    }

    public void addNode(OSMNode node) {
        if (node.isBikeParking()) {
            bikeParkingNodes.put(node.getId(), node);
        }
        if (node.isParkAndRide()) {
            carParkingNodes.put(node.getId(), node);
        }
        if (!(waysNodeIds.contains(node.getId()) || areaNodeIds.contains(node.getId()) || node
                .isStop())) {
            return;
        }

        if (nodesById.containsKey(node.getId())) {
            return;
        }

        nodesById.put(node.getId(), node);

        if (nodesById.size() % 100000 == 0) {
            LOG.debug("nodes=" + nodesById.size());
        }
    }

    public void addWay(OSMWay way) {
        /* only add ways once */
        long wayId = way.getId();
        if (waysById.containsKey(wayId) || areaWaysById.containsKey(wayId)) {
            return;
        }

        if (areaWayIds.contains(wayId)) {
            areaWaysById.put(wayId, way);
        }

        /* filter out ways that are not relevant for routing */
        if (!(OSMFilter.isWayRoutable(way) || way.isParkAndRide() || way.isBikeParking())) {
            return;
        }

        applyLevelsForWay(way);

        /* An area can be specified as such, or be one by default as an amenity */
        if ((way.isTag("area", "yes") || way.isTag("amenity", "parking") || way.isTag("amenity",
                "bicycle_parking")) && way.getNodeRefs().size() > 2) {
            // this is an area that's a simple polygon. So we can just add it straight
            // to the areas, if it's not part of a relation.
            if (!areaWayIds.contains(wayId)) {
                singleWayAreas.add(way);
                areaWaysById.put(wayId, way);
                areaWayIds.add(wayId);
                way.getNodeRefs().forEach(node -> {
                    MapUtils.addToMapSet(areasForNode, node, way);
                    return true;
                });
            }
            return;
        }

        waysById.put(wayId, way);

        if (waysById.size() % 10000 == 0) {
            LOG.debug("ways=" + waysById.size());
        }
    }

    public void addRelation(OSMRelation relation) {
        if (relationsById.containsKey(relation.getId())) {
            return;
        }

        if (relation.isTag("type", "multipolygon")
                && (OSMFilter.isOsmEntityRoutable(relation) || relation.isParkAndRide()) || relation.isBikeParking()) {
            // OSM MultiPolygons are ferociously complicated, and in fact cannot be processed
            // without reference to the ways that compose them. Accordingly, we will merely
            // mark the ways for preservation here, and deal with the details once we have
            // the ways loaded.
            if (!OSMFilter.isWayRoutable(relation) && !relation.isParkAndRide() && !relation.isBikeParking()) {
                return;
            }
            for (OSMRelationMember member : relation.getMembers()) {
                areaWayIds.add(member.getRef());
            }
            applyLevelsForWay(relation);
        } else if (!(relation.isTag("type", "restriction"))
                && !(relation.isTag("type", "route") && relation.isTag("route", "road"))
                && !(relation.isTag("type", "multipolygon") && OSMFilter
                        .isOsmEntityRoutable(relation))
                && !(relation.isTag("type", "level_map"))
                && !(relation.isTag("type", "public_transport") && relation.isTag(
                        "public_transport", "stop_area"))
                && !(relation.isTag("type", "route") && (
                        relation.isTag("route", "road") ||
                        relation.isTag("route", "bicycle")))){
            return;
        }

        relationsById.put(relation.getId(), relation);

        if (relationsById.size() % 100 == 0) {
            LOG.debug("relations=" + relationsById.size());
        }
    }

    /**
     * Called after the first phase, when all relations are loaded.
     */
    public void doneFirstPhaseRelations() {
        // nothing to do here
    }

    /**
     * Called after the second phase, when all ways are loaded.
     */
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

    /**
     * Called after the third and final phase, when all nodes are loaded.
     * After all relations, ways, and nodes are loaded, handle areas.
     */
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

    // Simple holder for the spatial index
    class RingSegment {
        Area area;

        Ring ring;

        OSMNode nA;

        OSMNode nB;
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
        Set<P2<Long>> commonSegments = new HashSet<>();
        HashGridSpatialIndex<RingSegment> spndx = new HashGridSpatialIndex<>();
        for (Area area : Iterables.concat(parkAndRideAreas, bikeParkingAreas)) {
            for (Ring ring : area.outermostRings) {
                processAreaRingForUnconnectedAreas(commonSegments, spndx, area, ring);
            }
        }

        // For each way, intersect with areas
        int nCreatedNodes = 0;
        for (OSMWay way : waysById.valueCollection()) {
            OSMLevel wayLevel = getLevelForWay(way);

            // For each segment of the way
            for (int i = 0; i < way.getNodeRefs().size() - 1; i++) {                
                OSMNode nA = nodesById.get(way.getNodeRefs().get(i));
                OSMNode nB = nodesById.get(way.getNodeRefs().get(i + 1));
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
                    if (ringSegment.nA.getId() == nA.getId()
                            || ringSegment.nA.getId() == nB.getId()
                            || ringSegment.nB.getId() == nA.getId()
                            || ringSegment.nB.getId() == nB.getId()) {
                        continue;
                    }
                    
                    // Skip if area and way are from "incompatible" levels
                    OSMLevel areaLevel = getLevelForWay(ringSegment.area.parent);
                    if (!wayLevel.equals(areaLevel)) {
                        continue;
                    }

                    // Check for real intersection
                    LineString seg2 = GeometryUtils.makeLineString(ringSegment.nA.lon,
                            ringSegment.nA.lat, ringSegment.nB.lon, ringSegment.nB.lat);
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
                                way, nA, nB, ringSegment.area.parent, ringSegment.nA,
                                ringSegment.nB, intersection
                        );
                        continue;
                    }
                    
                    // if the intersection is extremely close to one of the nodes of the road or the parking lot, just use that node
                    // rather than splitting anything. See issue 1605.
                    OSMNode splitNode;
                    double epsilon = 0.0000001;
                    
                    // note that the if . . . else if structure of this means that if a node at one end of a (way|ring) segment is snapped,
                    // the node at the other end cannot be, which is fine because the only time that could happen anyhow
                    // would be if the nodes were duplicates.
                    // prefer inserting into the ring segment to inserting into the way, so as to reduce graph complexity
                    if (checkIntersectionDistance(p, nA, epsilon)) {
                    	// insert node A into the ring segment
                        splitNode = nA;
                        
                        if (ringSegment.ring.nodes.contains(splitNode))
                        	// This node is already a part of this ring (perhaps we inserted it previously). No need to connect again.
                        	// Note that this may not be a safe assumption to make in all cases; suppose a way were to cross exactly over a node *twice*,
                        	// we would only add it the first time.
                        {
                            continue;
                        }
                        
                        if (checkDistanceWithin(ringSegment.nA, nA, epsilon) || checkDistanceWithin(ringSegment.nB, nA, epsilon)) {
                            LOG.info("Node {} in way {} is coincident but disconnected with area {}",
                                    nA.getId(), way.getId(), ringSegment.area.parent.getId());
                        }
                    }
                    else if (checkIntersectionDistance(p, nB, epsilon)) {
                    	// insert node B into the ring segment
                        splitNode = nB;
                        
                        if (ringSegment.ring.nodes.contains(splitNode)) {
                            continue;
                        }
                        
                        if (checkDistanceWithin(ringSegment.nA, nB, epsilon) || checkDistanceWithin(ringSegment.nB, nB, epsilon)) {
                            LOG.info("Node {} in way {} is coincident but disconnected with area {}",
                                	nB.getId(), way.getId(), ringSegment.area.parent.getId());
                        }
                    }
                    else if (checkIntersectionDistance(p, ringSegment.nA, epsilon)) {
                    	// insert node A into the road, if it's not already there
                    	
                    	// don't insert the same node twice. This is not always safe; suppose a way crosses over the same node in the parking area twice.
                    	// but we assume it doesn't (and even if it does, it's not a huge deal, as it is still connected elsewhere on the same way).
                    	if (way.getNodeRefs().contains(ringSegment.nA.getId()))
                    		continue;
                    	
                    	way.addNodeRef(ringSegment.nA.getId(), i + 1);
                    	
                        if (
                                checkDistanceWithin(ringSegment.nA, nA, epsilon) ||
                                checkDistanceWithin(ringSegment.nA, nB, epsilon)
                        ) {
                            LOG.info(
                                    "Node {} in area {} is coincident but disconnected with way {}",
                                    ringSegment.nA.getId(),
                                    ringSegment.area.parent.getId(),
                                    way.getId()
                            );
                        }
                    	// restart loop over way segments as we may have more intersections
                        // as we haven't modified the ring, there is no need to modify the spatial index, so breaking here is fine 
                    	i--;
                    	break;
                    }
                    else if (checkIntersectionDistance(p, ringSegment.nB, epsilon)) {
                    	// insert node B into the road, if it's not already there
                    	
                    	if (way.getNodeRefs().contains(ringSegment.nB.getId()))
                    		continue;
                    	
                    	way.addNodeRef(ringSegment.nB.getId(), i + 1);
                    	
                        if (
                                checkDistanceWithin(ringSegment.nB, nA, epsilon) ||
                                checkDistanceWithin(ringSegment.nB, nB, epsilon)
                        ) {
                            LOG.info("Node {} in area {} is coincident but disconnected with way {}",
                                    ringSegment.nB.getId(),
                                    ringSegment.area.parent.getId(),
                                    way.getId()
                            );
                        }
                    	i--;
                    	break;
                    }
                    else {
                    	// create a node
                    	splitNode = createVirtualNode(p.getCoordinate());
                        nCreatedNodes++;
                        LOG.debug(
                                "Adding virtual {}, intersection of {} ({}--{}) and area {} ({}--{}) at {}.",
                                splitNode, way, nA, nB, ringSegment.area.parent, ringSegment.nA,
                                ringSegment.nB, p);
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
                    Envelope env2 = new Envelope(ringSegment2.nA.lon, ringSegment2.nB.lon,
                            ringSegment2.nA.lat, ringSegment2.nB.lat);
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
            Set<P2<Long>> commonSegments,
            HashGridSpatialIndex<RingSegment> spndx,
            Area area,
            Ring ring
    ) {
        for (int j = 0; j < ring.nodes.size(); j++) {
            RingSegment ringSegment = new RingSegment();
            ringSegment.area = area;
            ringSegment.ring = ring;
            ringSegment.nA = ring.nodes.get(j);
            ringSegment.nB = ring.nodes.get((j + 1) % ring.nodes.size());
            Envelope env = new Envelope(ringSegment.nA.lon, ringSegment.nB.lon,
                    ringSegment.nA.lat, ringSegment.nB.lat);
            P2<Long> key1 = new P2<>(ringSegment.nA.getId(), ringSegment.nB.getId());
            P2<Long> key2 = new P2<>(ringSegment.nB.getId(), ringSegment.nA.getId());
            if (!commonSegments.contains(key1) && !commonSegments.contains(key2)) {
                spndx.insert(env, ringSegment);
                commonSegments.add(key1);
                commonSegments.add(key2);
            }
        }

        ring.getHoles().forEach(hole -> processAreaRingForUnconnectedAreas(commonSegments, spndx, area, hole));
    }

    /**
     * Create a virtual OSM node, using a negative unique ID.
     * 
     * @param c The location of the node to create.
     * @return The created node.
     */
    private OSMNode createVirtualNode(Coordinate c) {
        OSMNode node = new OSMNode();
        node.lon = c.x;
        node.lat = c.y;
        node.setId(virtualNodeId);
        virtualNodeId--;
        waysNodeIds.add(node.getId());
        nodesById.put(node.getId(), node);
        return node;
    }

    private void applyLevelsForWay(OSMWithTags way) {
        /* Determine OSM level for each way, if it was not already set */
        if (!wayLevels.containsKey(way)) {
            // if this way is not a key in the wayLevels map, a level map was not
            // already applied in processRelations

            /* try to find a level name in tags */
            String levelName = null;
            OSMLevel level = OSMLevel.DEFAULT;
            if (way.hasTag("level")) { // TODO: floating-point levels &c.
                levelName = way.getTag("level");
                level = OSMLevel.fromString(levelName, OSMLevel.Source.LEVEL_TAG, noZeroLevels,
                    issueStore
                );
            } else if (way.hasTag("layer")) {
                levelName = way.getTag("layer");
                level = OSMLevel.fromString(levelName, OSMLevel.Source.LAYER_TAG, noZeroLevels,
                    issueStore
                );
            }
            if (level == null || (!level.reliable)) {
                issueStore.add(new LevelAmbiguous(levelName, way.getId()));
                level = OSMLevel.DEFAULT;
            }
            wayLevels.put(way, level);
        }
    }

    private void markNodesForKeeping(Collection<OSMWay> osmWays, TLongSet nodeSet) {
        for (Iterator<OSMWay> it = osmWays.iterator(); it.hasNext();) {
            OSMWay way = it.next();
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
        AREA: for (OSMWay way : singleWayAreas) {
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
                newArea(new Area(way, Arrays.asList(way), Collections.emptyList(), nodesById));
            } catch (Area.AreaConstructionException|Ring.RingConstructionException e) {
                // this area cannot be constructed, but we already have all the
                // necessary nodes to construct it. So, something must be wrong with
                // the area; we'll mark it as processed so that we don't retry.
                issueStore.add("InvalidGeometry", "Invalid geometry for osm way %s", way.getId());
            } catch (IllegalArgumentException iae) {
                // This occurs when there are an invalid number of points in a LinearRing
                // Mark the ring as processed so we don't retry it.
                issueStore.add("InvalidGeometry", "Invalid geometry for osm way %s", way.getId());
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
        RELATION: for (OSMRelation relation : relationsById.valueCollection()) {
            if (processedAreas.contains(relation)) {
                continue;
            }
            if (!(relation.isTag("type", "multipolygon") && (OSMFilter
                    .isOsmEntityRoutable(relation) || relation.isParkAndRide() || relation.isBikeParking()))) {
                continue;
            }
            // Area multipolygons -- pedestrian plazas
            ArrayList<OSMWay> innerWays = new ArrayList<OSMWay>();
            ArrayList<OSMWay> outerWays = new ArrayList<OSMWay>();
            for (OSMRelationMember member : relation.getMembers()) {
                String role = member.getRole();
                OSMWay way = areaWaysById.get(member.getRef());
                if (way == null) {
                    // relation includes way which does not exist in the data. Skip.
                    continue RELATION;
                }
                TLongIterator wayNodeIterator = way.getNodeRefs().iterator();
                while (wayNodeIterator.hasNext()) {
                    long nodeId = wayNodeIterator.next();
                    if (nodesById.containsKey(nodeId)) {
                        MapUtils.addToMapSet(areasForNode, nodeId, way);
                    } else {
                        // this area is missing some nodes, perhaps because it is on
                        // the edge of the region, so we will simply not route on it.
                        continue RELATION;
                    }
                }
                if (role.equals("inner")) {
                    innerWays.add(way);
                } else if (role.equals("outer")) {
                    outerWays.add(way);
                } else {
                    LOG.warn("Unexpected role " + role + " in multipolygon");
                }
            }
            processedAreas.add(relation);
            try {
                newArea(new Area(relation, outerWays, innerWays, nodesById));
            } catch (Area.AreaConstructionException|Ring.RingConstructionException e) {
                issueStore.add("InvalidGeometry", "Invalid geometry for osm relation %s", relation.getId());
                continue;
            }

            for (OSMRelationMember member : relation.getMembers()) {
                // multipolygons for attribute mapping
                if (!("way".equals(member.getType()) && waysById.containsKey(member.getRef()))) {
                    continue;
                }

                OSMWithTags way = waysById.get(member.getRef());
                if (way == null) {
                    continue;
                }
                String[] relationCopyTags = { "highway", "name", "ref" };
                for (String tag : relationCopyTags) {
                    if (relation.hasTag(tag) && !way.hasTag(tag)) {
                        way.addTag(tag, relation.getTag(tag));
                    }
                }
                if (relation.isTag("railway", "platform") && !way.hasTag("railway")) {
                    way.addTag("railway", "platform");
                }
                if (relation.isTag("public_transport", "platform")
                        && !way.hasTag("public_transport")) {
                    way.addTag("public_transport", "platform");
                }
            }
        }
    }

    /**
     * Handler for a new Area (single way area or multipolygon relations)
     */
    private void newArea(Area area) {
        StreetTraversalPermission permissions = OSMFilter.getPermissionsForEntity(area.parent,
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        if (OSMFilter.isOsmEntityRoutable(area.parent)
                && permissions != StreetTraversalPermission.NONE) {
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

        for (OSMRelation relation : relationsById.valueCollection()) {
            if (relation.isTag("type", "restriction")) {
                processRestriction(relation);
            } else if (relation.isTag("type", "level_map")) {
                processLevelMap(relation);
            } else if (relation.isTag("type", "route")) {
                processRoad(relation);
                processBicycleRoute(relation);
            } else if (relation.isTag("type", "public_transport")) {
                processPublicTransportStopArea(relation);
            }
        }
    }

    /**
     * Handle route=bicycle relations. Copies their network type to all way members.
     *
     * @see "https://wiki.openstreetmap.org/wiki/Tag:route%3Dbicycle"
     */
    private void processBicycleRoute(OSMRelation relation) {
        if (relation.isTag("route", "bicycle")) {
            var network = relation.getTag("network");

            if (network == null)
                network = "lcn";
            switch (network) {
                case "lcn": setNetworkForAllMembers(relation, "lcn"); break;
                case "rcn": setNetworkForAllMembers(relation, "rcn"); break;
                case "ncn": setNetworkForAllMembers(relation, "ncn"); break;
                case "icn": setNetworkForAllMembers(relation, "icn"); break;
                // we treat networks without known network type like local networks
                default: setNetworkForAllMembers(relation, "lcn"); break;
            }
        }
    }

    private void setNetworkForAllMembers(OSMRelation relation, String key) {
        relation.getMembers().forEach(member -> {
            var isOsmWay = "way".equals(member.getType());
            var way = waysById.get(member.getRef());
            // if it is an OSM way (rather than a node) and it it doesn't already contain the tag
            // we add it
            if (way != null && isOsmWay && !way.hasTag(key)) {
                way.addTag(key, "yes");
            }
        });
    }

    /**
     * Store turn restrictions.
     */
    private void processRestriction(OSMRelation relation) {
        long from = -1, to = -1, via = -1;
        for (OSMRelationMember member : relation.getMembers()) {
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
            issueStore.add(new TurnRestrictionBad(relation.getId(),
                "One of from|via|to edges are empty in relation"));
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
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.RIGHT,
                relation.getId());
        } else if (relation.isTag("restriction", "no_left_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.LEFT,
                relation.getId());
        } else if (relation.isTag("restriction", "no_straight_on")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.STRAIGHT,
                relation.getId());
        } else if (relation.isTag("restriction", "no_u_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.U,
                relation.getId());
        } else if (relation.isTag("restriction", "only_straight_on")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.STRAIGHT,
                relation.getId());
        } else if (relation.isTag("restriction", "only_right_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.RIGHT,
                relation.getId());
        } else if (relation.isTag("restriction", "only_left_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.LEFT,
                relation.getId());
        } else if (relation.isTag("restriction", "only_u_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.U,
                relation.getId());
        } else {
            issueStore.add(new TurnRestrictionUnknown(relation.getId(), relation.getTag("restriction")));
            return;
        }
        tag.modes = modes.clone();

        // set the time periods for this restriction, if applicable
        if (relation.hasTag("day_on") && relation.hasTag("day_off") && relation.hasTag("hour_on")
                && relation.hasTag("hour_off")) {

            try {
                tag.time = RepeatingTimePeriod.parseFromOsmTurnRestriction(
                        relation.getTag("day_on"), relation.getTag("day_off"),
                        relation.getTag("hour_on"), relation.getTag("hour_off"));
            } catch (NumberFormatException e) {
                LOG.info("Unparseable turn restriction: " + relation.getId());
            }
        }

        turnRestrictionsByFromWay.put(from, tag);
        turnRestrictionsByToWay.put(to, tag);
    }

    /**
     * Process an OSM level map.
     */
    private void processLevelMap(OSMRelation relation) {
        Map<String, OSMLevel> levels = OSMLevel.mapFromSpecList(relation.getTag("levels"),
                Source.LEVEL_MAP, true, issueStore
        );
        for (OSMRelationMember member : relation.getMembers()) {
            if ("way".equals(member.getType()) && waysById.containsKey(member.getRef())) {
                OSMWay way = waysById.get(member.getRef());
                if (way != null) {
                    String role = member.getRole();
                    // if the level map relation has a role:xyz tag, this way is something
                    // more complicated than a single level (e.g. ramp/stairway).
                    if (!relation.hasTag("role:" + role)) {
                        if (levels.containsKey(role)) {
                            wayLevels.put(way, levels.get(role));
                        } else {
                            LOG.warn(member.getRef() + " has undefined level " + role);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle route=road relations.
     */
    private void processRoad(OSMRelation relation) {
        for (OSMRelationMember member : relation.getMembers()) {
            if (!("way".equals(member.getType()) && waysById.containsKey(member.getRef()))) {
                continue;
            }

            OSMWithTags way = waysById.get(member.getRef());
            if (way == null) {
                continue;
            }

            if (relation.hasTag("name")) {
                if (way.hasTag("otp:route_name")) {
                    way.addTag("otp:route_name",
                            addUniqueName(way.getTag("otp:route_name"), relation.getTag("name")));
                } else {
                    way.addTag(new OSMTag("otp:route_name", relation.getTag("name")));
                }
            }
            if (relation.hasTag("ref")) {
                if (way.hasTag("otp:route_ref")) {
                    way.addTag("otp:route_ref",
                            addUniqueName(way.getTag("otp:route_ref"), relation.getTag("ref")));
                } else {
                    way.addTag(new OSMTag("otp:route_ref", relation.getTag("ref")));
                }
            }
        }
    }

    /**
     * Process an OSM public transport stop area relation.
     * 
     * This goes through all public_transport=stop_area relations and adds the parent (either an
     * area or multipolygon relation) as the key and a Set of transit stop nodes that should be
     * included in the parent area as the value into stopsInAreas. This improves
     * TransitToTaggedStopsGraphBuilder by enabling us to have unconnected stop nodes within the
     * areas by creating relations .
     * 
     * @author hannesj
     * @see "http://wiki.openstreetmap.org/wiki/Tag:public_transport%3Dstop_area"
     */
    private void processPublicTransportStopArea(OSMRelation relation) {
        OSMWithTags platformArea = null;
        Set<OSMNode> platformsNodes = new HashSet<>();
        for (OSMRelationMember member : relation.getMembers()) {
            if ("way".equals(member.getType()) && "platform".equals(member.getRole())
                    && areaWayIds.contains(member.getRef())) {
                if (platformArea == null) {
                    platformArea = areaWaysById.get(member.getRef());
                } else {
                    issueStore.add(new TooManyAreasInRelation(relation.getId()));
                }
            } else if ("relation".equals(member.getType()) && "platform".equals(member.getRole())
                    && relationsById.containsKey(member.getRef())) {
                if (platformArea == null) {
                    platformArea = relationsById.get(member.getRef());
                } else {
                    issueStore.add(new TooManyAreasInRelation(relation.getId()));
                }
            } else if ("node".equals(member.getType()) && nodesById.containsKey(member.getRef())) {
                platformsNodes.add(nodesById.get(member.getRef()));
            }
        }
        if (platformArea != null && !platformsNodes.isEmpty()) {
            stopsInAreas.put(platformArea, platformsNodes);
        } else {
            issueStore.add(new PublicTransportRelationSkipped(relation.getId()));
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

    /**
     * Check if a point is within an epsilon of a node.
     */
    private static boolean checkIntersectionDistance(Point p, OSMNode n, double epsilon) {
    	return Math.abs(p.getY() - n.lat) < epsilon && Math.abs(p.getX() - n.lon) < epsilon;
    }

    /**
     * Check if two nodes are within an epsilon.
     */
    private static boolean checkDistanceWithin(OSMNode a, OSMNode b, double epsilon) {
    	return Math.abs(a.lat - b.lat) < epsilon && Math.abs(a.lon - b.lon) < epsilon;
	}
}
