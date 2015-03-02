/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Longs;
import com.vividsolutions.jts.geom.*;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.opentripplanner.common.RepeatingTimePeriod;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.*;
import org.opentripplanner.graph_builder.module.osm.TurnRestrictionTag.Direction;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMLevel.Source;
import org.opentripplanner.osm.*;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * abyrd: I am turning this into a set of indexes wrapping a new MapDB OSM dataset.
 * This used to be an implementation of OSMContentHandler (now called OSMStorage) but it's going to be the only
 * implementation so I've cut it off from that interface.
 */
public class OSMDatabase {

    private static Logger LOG = LoggerFactory.getLogger(OSMDatabase.class);

    /* The new-style MapDB-based OSM dataset that is indexed in this wrapper class. */
    public OSM osm;

    /* Map of all bike-rental nodes, keyed by their OSM ID */ // TODO use Trove TLongSet? Be careful of get()==null.
    public final Set<Long> bikeRentalNodes = Sets.newHashSet();

    /* Map of all bike parking nodes, keyed by their OSM ID */
    public final Set<Long> bikeParkingNodes = Sets.newHashSet();

    /* Set of IDs of all area ways. */
    public final Set<Long> areaWayIds = Sets.newHashSet();

    /* All walkable areas. These are OTP Area objects, not the source way or relation IDs. */
    public final List<Area> walkableAreas = new ArrayList<Area>();

    /* All P+R area objects (for visibility graph construction) */
    public final List<Area> parkAndRideAreas = new ArrayList<Area>();

    /* All bike parking area objects (for visibility graph construction) */
    public final List<Area> bikeParkingAreas = new ArrayList<Area>();

    /* Map of all area OSMWay for a given node */
    public final Multimap<Long, Long> areasContainingNode = ArrayListMultimap.create();

    /* Set of IDs for areas that are composed of a single OSM Way. */
    public final Set<Long> singleWayAreas = Sets.newHashSet();

    /* Track which ways and relations have been processed as areas (TODO define "processed") */
    // TODO use more trove collections elsewhere
    private TLongSet processedAreaWays = new TLongHashSet();
    private TLongSet processedAreaRelations = new TLongHashSet();

    /* Set of all node IDs of kept ways. Needed to mark which nodes to keep in stage 3. */
    public final Set<Long> waysNodeIds = new HashSet<Long>(); // TODO change name, it's confusing

    /* Set of all node IDs of kept areas. Needed to mark which nodes to keep in stage 3. */
    public final Set<Long> areaNodeIds = new HashSet<Long>();

    /* Track which vertical level each OSM way belongs to, for building elevators etc. */
    public final Map<Long, OSMLevel> wayLevels = Maps.newHashMap();
    // TODO make another one for relations, use Trove w/ defaults

    /* Set of turn restrictions for each turn "from" way ID */
    public final Multimap<Long, TurnRestrictionTag> turnRestrictionsByFromWay = ArrayListMultimap.create();

    /* Set of turn restrictions for each turn "to" way ID */
    public final Multimap<Long, TurnRestrictionTag> turnRestrictionsByToWay = ArrayListMultimap.create();

    /*
     * Map of all transit stop nodes that lie within an area and which are connected to the area by
     * a relation. Keyed on the area's OSM way.
     * FIXME actually it was keyed on any OSM entity, with identity equality.
     * This is because the areas can come from ways or relations. Could we use the Area object itself?
     * In its current state this can't work because the key is a transient OSM object rather than a true key.
     */
    public final Map<Tagged, Set<Long>> stopsInAreas = Maps.newHashMap();

    /* List of graph annotations registered during building, to add to the graph. */
    private List<GraphBuilderAnnotation> annotations = new ArrayList<>();

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

    public OSMDatabase (OSM osm) {
        this.osm = osm;
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

    public boolean isNodeSharedByMultipleAreas(Long nodeId) {
        Collection<Long> areas = areasContainingNode.get(nodeId);
        if (areas == null) {
            return false;
        }
        return areas.size() > 1;
    }

    public boolean isNodeBelongsToWay(Long nodeId) {
        return waysNodeIds.contains(nodeId);
    }

    public Collection<GraphBuilderAnnotation> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    // abyrd: OK updated for new MapDB OSM
    // TODO this is really minimal work, and there are a lot of nodes. Time this operation and maybe perform it elsewhere.
    /** Called repeatedly with every OSM Node during the third loading phase. */
    public void addNode(long nodeId, Node node) {
        if (OSMFilter.isBikeRental(node)) {
            bikeRentalNodes.add(nodeId);
        } else if (OSMFilter.isBikeParking(node)) {
            bikeParkingNodes.add(nodeId);
        }
    }

    // abyrd: OK updated for new MapDB OSM
    /** Called repeatedly with every OSM Way during the second loading phase. */
    public void addWay(long wayId, Way way) {

        applyLevelsForWay(wayId, way);

        /* Filter out ways that are not relevant for routing. */
        if (!(OSMFilter.isWayRoutable(way) || OSMFilter.isParkAndRide(way) || OSMFilter.isBikeParking(way))) {
            osm.ways.remove(wayId);
            return;
        }

        /* A way may be explicitly tagged as an area, or an amenity tag may imply that it is an area. */
        if ((way.hasTag("area", "yes") || way.hasTag("amenity", "parking")
            || way.hasTag("amenity", "bicycle_parking")) && way.nodes.length > 2) {
            // this is an area that's a simple polygon. So we can just add it straight
            // to the areas, if it's not part of a relation.
            // TODO was there a check above whether this way belonged to a multipolygon or relation?
            // perhaps the fact that it doesn't appear yet in the map means it wasn't set by the relation handler?
            if (!areaWayIds.contains(wayId)) {
                singleWayAreas.add(wayId);
                areaWayIds.add(wayId);
                for (long nodeId : way.nodes) {
                    areasContainingNode.put(nodeId, wayId);
                }
            }
            osm.ways.remove(wayId); // this is imitating existing behavior... but why were we doing it?
        }

    }

    // abyrd: OK updated for new MapDB OSM
    /** Called repeatedly with every OSM Relation during the first loading phase. */
    public void addRelation(long relationId, Relation relation) {

        if (relation.hasTag("type", "multipolygon") &&
            (OSMFilter.isOsmEntityRoutable(relation) || OSMFilter.isParkAndRide(relation))) {
            // OSM MultiPolygons are ferociously complicated, and in fact cannot be processed
            // without reference to the ways that compose them. Accordingly, we will merely
            // mark the ways for preservation here, and deal with the details once we have
            // the ways loaded.
            if (!OSMFilter.isWayRoutable(relation) && ! OSMFilter.isParkAndRide(relation)) {
                return;
            }
            for (Relation.Member member : relation.members) {
                if (member.type == Relation.Type.WAY) {
                    areaWayIds.add(member.id);
                }
            }
            applyLevelsForWay(relationId, relation);
            return;
        }
        if ( ! (relation.hasTag("type", "restriction")
            || (relation.hasTag("type", "route") && relation.hasTag("route", "road"))
            || (relation.hasTag("type", "level_map"))
            || (relation.hasTag("type", "public_transport") && relation.hasTag("public_transport", "stop_area")))) {
            // remove relation, it's not interesting
            osm.relations.remove(relationId);
        }
    }

    public void doneFirstPhaseRelations() {
        // TODO There is no special work to be done after the first phase, maybe eliminate this interface function
    }

    public void doneSecondPhaseWays() {
        // TODO There is no special work to be done after the second phase, maybe eliminate this interface function
        // It used to "mark nodes for keeping" which makes no sense when using a random-access MapDB
    }

    /**
     * After all relations, ways, and nodes are loaded, handle multipolygon and single-way areas.
     * TODO merge this into the post-load function and remove the possibility of multiple OSM sources
     */
    public void doneThirdPhaseNodes() {
        // This copies tags (highway=*) such that nodes are not later purged. (abyrd: Why? What does this mean?)
        // Multipolygons may be processed more than once, which may be needed since
        // some member might be in different files for the same multipolygon.
        // NOTE (abyrd): this purging phase may not be necessary if highway tags are not
        // copied over from multipolygon relations. Perhaps we can get by with
        // only 2 steps -- ways+relations, followed by used nodes.
        // Ways can be tag-filtered in phase 1.
        processMultipolygonRelations();
        processSingleWayAreas();
    }

    /**
     * After all loading is done (from multiple OSM sources), post-process.
     * TODO merge this into third-phase-done function, and remove the possibility of multiple OSM sources
     */
    public void postLoad() {

        // handle turn restrictions, road names, and level maps in relations
        processRelations();

        // intersect non connected areas with ways
        processUnconnectedAreas();
    }

    /**
     * An "unconnected area" is an area whose outer ring crosses one or more ways on the same OSM level without
     * sharing any nodes. Currently we process only park and ride areas, but this could easily be extended to
     * other areas as well.
     */
    private void processUnconnectedAreas() {
        LOG.info("Intersecting disconnected OSM areas with nearby ways...");

        // Simple holder for the spatial index
        class RingSegment {
            Area area;
            Ring ring;
            long nAid;
            Node nA;
            long nBid;
            Node nB;
        }

        /*
         * Create a spatial index for each segment of area outer rings. Note: The spatial index is
         * temporary and store only areas, so it should not take that much memory. Note 2: For
         * common nodes shared by different ways of different areas we only add them once, otherwise
         * we could end-up looping on creating new intersections.
         */
        Set<P2<Long>> commonSegments = new HashSet<P2<Long>>();
        HashGridSpatialIndex<RingSegment> spndx = new HashGridSpatialIndex<>();
        for (Area area : Iterables.concat(parkAndRideAreas, bikeParkingAreas)) {
            for (Ring ring : area.outermostRings) {
                for (int j = 0; j < ring.nodeIds.size(); j++) {
                    RingSegment ringSegment = new RingSegment(); // TODO RingSegment(area, ring, i);
                    ringSegment.area = area;
                    ringSegment.ring = ring;
                    ringSegment.nAid = ring.nodeIds.get(j);
                    ringSegment.nA = osm.nodes.get(ringSegment.nAid);
                    ringSegment.nBid = ring.nodeIds.get((j + 1) % ring.nodeIds.size());
                    ringSegment.nB = osm.nodes.get(ringSegment.nBid);
                    Envelope env = new Envelope(ringSegment.nA.getLon(), ringSegment.nB.getLat(),
                            ringSegment.nA.getLat(), ringSegment.nB.getLon());
                    P2<Long> key1 = new P2<>(ringSegment.nAid, ringSegment.nBid);
                    P2<Long> key2 = new P2<>(ringSegment.nBid, ringSegment.nAid);
                    if (!commonSegments.contains(key1) && !commonSegments.contains(key2)) {
                        spndx.insert(env, ringSegment);
                        commonSegments.add(key1);
                        commonSegments.add(key2);
                    }
                }
            }
        }

        // For each way, intersect with areas
        int nCreatedNodes = 0;
        for (Map.Entry<Long, Way> entry : osm.ways.entrySet()) {
            long wayId = entry.getKey();
            Way way = entry.getValue();
            OSMLevel wayLevel = wayLevels.get(wayId);

            // For each segment of the way
            for (int i = 0; i < way.nodes.length - 1; i++) {
                long nAid = way.nodes[i];
                long nBid = way.nodes[i + 1];
                Node nA = osm.nodes.get(nAid);
                Node nB = osm.nodes.get(nBid);
                if (nA == null || nB == null) {
                    continue;
                }
                Envelope env = new Envelope(nA.getLon(), nB.getLon(), nA.getLat(), nB.getLat());
                List<RingSegment> ringSegments = spndx.query(env);
                if (ringSegments.size() == 0)
                    continue;
                LineString seg = GeometryUtils.makeLineString(nA.getLon(), nA.getLat(), nB.getLon(), nB.getLat());
                
                for (RingSegment ringSegment : ringSegments) {
                	boolean wayWasSplit = false;

                    // Skip if both segments share a common node
                    if (ringSegment.nAid == nAid
                            || ringSegment.nAid == nBid
                            || ringSegment.nBid == nAid
                            || ringSegment.nBid == nBid)
                        continue;
                    
                    // Skip if area and way are from "incompatible" levels
                    OSMLevel areaLevel = wayLevels.get(ringSegment.area.describeParent());
                    if (!wayLevel.equals(areaLevel))
                        continue;

                    // Check for real intersection
                    LineString seg2 = GeometryUtils.makeLineString(
                            ringSegment.nA.getLon(), ringSegment.nA.getLat(),
                            ringSegment.nB.getLon(), ringSegment.nB.getLat());
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
                        LOG.error("Alien intersection type between {} ({}--{}) and {} ({}--{}): ",
                                way, nA, nB, ringSegment.area.parent, ringSegment.nA,
                                ringSegment.nB, intersection);
                        continue;
                    }
                    
                    // if the intersection is extremely close to one of the nodes of the road or the parking lot,
                    // just use that node rather than splitting anything. See issue 1605.
                    long splitNodeId;
                    double epsilon = 0.0000001;
                    
                    // note that the if . . . else if structure of this means that if a node at one end of a (way|ring) segment is snapped,
                    // the node at the other end cannot be, which is fine because the only time that could happen anyhow
                    // would be if the nodes were duplicates.
                    // prefer inserting into the ring segment to inserting into the way, so as to reduce graph complexity
                    if (checkIntersectionDistance(p, nA, epsilon)) {
                    	// insert node A into the ring segment
                        splitNodeId = nAid;
                        
                        if (ringSegment.ring.nodeIds.contains(splitNodeId))
                        	// This node is already a part of this ring (perhaps we inserted it previously). No need to connect again.
                        	// Note that this may not be a safe assumption to make in all cases; suppose a way were to cross exactly over a node *twice*,
                        	// we would only add it the first time.
                        	continue;
                        
                        if (checkDistance(ringSegment.nA, nA, epsilon) || checkDistance(ringSegment.nB, nA, epsilon))
                                LOG.info("Node {} in way {} is coincident but disconnected with area {}",
                                    	nAid, wayId, ringSegment.area.describeParent());
                    }
                    else if (checkIntersectionDistance(p, nB, epsilon)) {
                    	// insert node B into the ring segment
                        splitNodeId = nBid;
                        
                        if (ringSegment.ring.nodeIds.contains(splitNodeId))
                        	continue;
                        
                        if (checkDistance(ringSegment.nA, nB, epsilon) || checkDistance(ringSegment.nB, nB, epsilon))
                            LOG.info("Node {} in way {} is coincident but disconnected with area {}",
                                	nBid, wayId, ringSegment.area.describeParent());
                    }
                    else if (checkIntersectionDistance(p, ringSegment.nA, epsilon)) {
                    	// insert node A into the road, if it's not already there
                    	// don't insert the same node twice. This is not always safe; suppose a way crosses over the same node in the parking area twice.
                    	// but we assume it doesn't (and even if it does, it's not a huge deal, as it is still connected elsewhere on the same way).
                    	if (Longs.contains(way.nodes, ringSegment.nAid))
                    		continue;
                    	
                    	insertNodeReference(wayId, way, ringSegment.nAid, i + 1);
                    	
                        if (checkDistance(ringSegment.nA, nA, epsilon) || checkDistance(ringSegment.nA, nB, epsilon))
                            LOG.info("Node {} in area {} is coincident but disconnected with way {}",
                                	ringSegment.nAid, wayId, ringSegment.area.describeParent(), wayId);
                    	
                    	// restart loop over way segments as we may have more intersections
                        // as we haven't modified the ring, there is no need to modify the spatial index, so breaking here is fine 
                    	i--;
                    	break;
                    }
                    else if (checkIntersectionDistance(p, ringSegment.nB, epsilon)) {
                    	// insert node B into the road, if it's not already there

                    	if (Longs.contains(way.nodes, ringSegment.nBid))
                    		continue;
                    	
                    	insertNodeReference(wayId, way, ringSegment.nBid, i + 1);
                    	
                        if (checkDistance(ringSegment.nB, nA, epsilon) || checkDistance(ringSegment.nB, nB, epsilon))
                            LOG.info("Node {} in area {} is coincident but disconnected with way {}",
                                	ringSegment.nBid, wayId, ringSegment.area.describeParent(), wayId);
                    	
                    	i--;
                    	break;
                    }
                    else {
                    	// Create a new node with a negative ID
                    	splitNodeId = createVirtualNode(p.getCoordinate());
                        nCreatedNodes++;
                        LOG.debug(
                                "Adding virtual node {}, intersection of {} ({}--{}) and area {} ({}--{}) at {}.",
                                splitNodeId, way, nA, nB, ringSegment.area.parent, ringSegment.nA,
                                ringSegment.nB, p);
                        insertNodeReference(wayId, way, splitNodeId, i + 1);
                        
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
                    int j = ringSegment.ring.nodeIds.indexOf(ringSegment.nBid);
                    ringSegment.ring.nodeIds.add(j, splitNodeId);
                    // TODO make this a method on Ring, especially if we keep parallel node and nodeId arrays in rings.

                    /*
                     * Update the spatial index because we just split a ring segment. Note: we do not update
                     * the first segment's envelope, but as the new envelope is smaller than the
                     * previous one this is harmless, apart from increasing the false positives count a bit.
                     */
                    RingSegment ringSegment2 = new RingSegment();
                    ringSegment2.area = ringSegment.area;
                    ringSegment2.ring = ringSegment.ring;
                    ringSegment2.nAid = splitNodeId;
                    ringSegment2.nA = osm.nodes.get(splitNodeId);
                    ringSegment2.nBid = ringSegment.nBid;
                    ringSegment2.nB = ringSegment.nB;
                    Envelope env2 = new Envelope(ringSegment2.nA.getLon(), ringSegment2.nB.getLon(),
                            ringSegment2.nA.getLat(), ringSegment2.nB.getLat());
                    spndx.insert(env2, ringSegment2);
                    ringSegment.nBid = splitNodeId;
                    ringSegment.nB = osm.nodes.get(splitNodeId);

                    // if we split the way, backtrack over it again to check for additional splits
                    // otherwise, we just continue the loop over ring segments
                    if (wayWasSplit) {
                    	i--;
                    	break;
                    }
                }
            }
        }
        LOG.info("Created {} virtual intersection nodes between areas and ways.", nCreatedNodes);
    }

	/**
     * Create a virtual OSM node, using a negative unique ID.
     * 
     * @param c The location of the node to create.
     * @return The created node's ID.
     */
    private long createVirtualNode(Coordinate c) {
        Node node = new Node(c.y, c.x);
        long nodeId = virtualNodeId--;
        waysNodeIds.add(nodeId);
        osm.nodes.put(nodeId, node);
        return nodeId;
    }

    /**
     * Insert a node ID in a way at the given index. This makes a round trip through a List and is therefore slow
     * but called very rarely. Side effect: this also updates the MapDB so it is coherent with the object on the heap.
     */
    public void insertNodeReference (long wayId, Way way, long nodeRef, int index) {
        List<Long> newNodes = Lists.newArrayList();
        newNodes.addAll(Longs.asList(way.nodes)); // fixed-length list backed by existing array, copy into new arraylist
        newNodes.add(index, nodeRef);
        way.nodes = Longs.toArray(newNodes);
        osm.ways.put(wayId, way);
    }


    // FIXME This function is being called on both relations and ways.
    // It used to work fine because the keys of the level-for-way map were OSM entity objects with identity equality.
    // Now the keys must be long integer IDs, which could (albeit rarely) lead to ID collisions across entity types.
    // FIXME this is not really "applying" the levels, it's recording them
    private void applyLevelsForWay(long entityId, Tagged entity) {

        /* Determine OSM level for each way, if it was not already set */
        if (!wayLevels.containsKey(entityId)) {
            // if this way is not a key in the wayLevels map, a level map was not
            // already applied in processRelations

            /* try to find a level name in tags */
            String levelName = null;
            OSMLevel level = OSMLevel.DEFAULT;
            if (entity.hasTag("level")) { // TODO: floating-point levels &c.
                levelName = entity.getTag("level");
                level = OSMLevel.fromString(levelName, OSMLevel.Source.LEVEL_TAG, noZeroLevels);
            } else if (entity.hasTag("layer")) {
                levelName = entity.getTag("layer");
                level = OSMLevel.fromString(levelName, OSMLevel.Source.LAYER_TAG, noZeroLevels);
            }
            if (level == null || (!level.reliable)) {
                // FIXME this may also be storing (and thus reporting) Relation IDs as Way IDs.
                LOG.warn(addBuilderAnnotation(new LevelAmbiguous(levelName, entityId)));
                level = OSMLevel.DEFAULT;
            }
            // FIXME this is storing a level for every way! Can we use a Trove map with DEFAULT value?
            wayLevels.put(entityId, level);
        }
    }

    /** Create Area objects from single ways (i.e. rather than multipolygon relations). */
    private void processSingleWayAreas() {
        AREA: for (long wayId : singleWayAreas) {
            Way way = osm.ways.get(wayId);
            if (processedAreaWays.contains(wayId)) { // why are we checking this? are some areas processed elsewhere?
                continue;
            }
            for (long nodeId : way.nodes) {
                if (!osm.nodes.containsKey(nodeId)) {
                    continue AREA;
                }
            }
            try {
                newArea(new Area(way, Collections.singletonList(wayId), Collections.<Long>emptyList(), osm));
            } catch (Area.AreaConstructionException e) {
                // this area cannot be constructed, but we already have all the
                // necessary nodes to construct it. So, something must be wrong with
                // the area; we'll mark it as processed so that we don't retry.
            } catch (IllegalArgumentException iae) {
                // This occurs when there are an invalid number of points in a LinearRing
                // Mark the ring as processed so we don't retry it.
            }
            processedAreaWays.add(wayId);
        }
    }

    /**
     * Copies useful metadata from multipolygon relations to the relevant ways, or to the area map.
     * TODO what is "the area map"? This is also obviously doing more than just copying the tags.
     * This is done at a different time than processRelations(), so that way purging doesn't remove
     * the used ways.
     * FIXME should there really be any "way purging" at all? Can't we just skip some ways based on their tags
     * while turning them into edges?
     */
    private void processMultipolygonRelations() {
        RELATION: for (Map.Entry<Long, Relation> entry : osm.relations.entrySet()) {
            long relationId = entry.getKey();
            Relation relation = entry.getValue();
            if (processedAreaRelations.contains(relationId)) {
                continue;
            }
            if (!(relation.hasTag("type", "multipolygon")
                    && (OSMFilter.isOsmEntityRoutable(relation) || OSMFilter.isParkAndRide(relation)))) {
                continue;
            }
            // Area multipolygons -- pedestrian plazas
            List<Long> innerWays = Lists.newArrayList();
            List<Long> outerWays = Lists.newArrayList();
            for (Relation.Member member : relation.members) {
                if (member.type != Relation.Type.WAY) {
                    // Below we expect the relation members to be ways. If one is not, skip this relation.
                    continue RELATION;
                }
                Way way = osm.ways.get(member.id);
                if (way == null) {
                    // This relation includes a way which does not exist in the input data. Skip it.
                    continue RELATION;
                }
                for (long nodeId : way.nodes) {
                    if ( ! osm.nodes.containsKey(nodeId)) {
                        // This area is missing some nodes, perhaps because it is on
                        // the edge of the region, so we will simply not route on it.
                        continue RELATION;
                    }
                    areasContainingNode.put(nodeId, member.id);
                }
                if (member.role.equalsIgnoreCase("inner")) {
                    innerWays.add(member.id);
                } else if (member.role.equalsIgnoreCase("outer")) {
                    outerWays.add(member.id);
                } else {
                    LOG.warn("Unexpected role '{}' in multipolygon", member.role);
                }
            }
            processedAreaRelations.add(relationId);
            try {
                newArea(new Area(relation, outerWays, innerWays, osm));
            } catch (Area.AreaConstructionException e) {
                continue;
            }

            for (Relation.Member member : relation.members) {
                // multipolygons for attribute mapping
                if (member.type != Relation.Type.WAY || ! osm.ways.containsKey(member.id)) {
                    continue;
                }
                Way way = osm.ways.get(member.id);
                if (way == null) {
                    continue;
                }
                for (String tagKey : new String[] {"highway", "name", "ref" }) {
                    String tagValue = relation.getTag(tagKey);
                    if (tagValue != null) {
                        setTagNoOverwrite(tagKey, tagValue, member.id, way);
                    }
                }
                if (relation.hasTag("railway", "platform")) {
                    setTagNoOverwrite("railway", "platform", member.id, way);
                }
                if (relation.hasTag("public_transport", "platform")) {
                    setTagNoOverwrite("public_transport", "platform", member.id, way);
                }
            }
        }
    }

    /**
     * Sets the given key-value pair on the given OSM entity, unless the key is already present.
     * Stores the way back to the MapDB as needed to keep it coherent with the Way object on the heap.
     * FIXME we should probably not be modifying tags on the source data. We could use a Map<long, Tagged.Tag>.
     * Set<Tag> inheritedTags, or map<String, String> inheritedTags.
     */
    public void setTagNoOverwrite (String key, String value, long wayId, Way way) {
        if ( ! way.hasTag(key)) {
            way.addTag(key, value);
            osm.ways.put(wayId, way);
        }
    }

    /** Handler for a new Area (from single-way areas or multipolygon relations) */
    private void newArea(Area area) {
        StreetTraversalPermission permissions = OSMFilter.getPermissionsForEntity(area.parent,
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        if (OSMFilter.isOsmEntityRoutable(area.parent) && permissions != StreetTraversalPermission.NONE) {
            walkableAreas.add(area);
        }
        // Please note: the same area can be both car P+R AND bike park.
        if (OSMFilter.isParkAndRide(area.parent)) {
            parkAndRideAreas.add(area);
        }
        if (OSMFilter.isBikeParking(area.parent)) {
            bikeParkingAreas.add(area);
        }
    }

    /**
     * Copies useful metadata (tags) from relations to their constituent ways and nodes.
     */
    private void processRelations() {
        LOG.info("Processing OSM relations...");
        for (Map.Entry<Long, Relation> entry : osm.relations.entrySet()) {
            long relationId = entry.getKey();
            Relation relation = entry.getValue();
            if (relation.hasTag("type", "restriction")) {
                processRestriction(relationId, relation);
            } else if (relation.hasTag("type", "level_map")) {
                processLevelMap(relationId, relation);
            } else if (relation.hasTag("type", "route")) {
                processRoad(relationId, relation);
            } else if (relation.hasTag("type", "public_transport")) {
                processPublicTransportStopArea(relationId, relation);
            }
        }
        LOG.debug("Done processing relations.");
    }

    /** Store turn restrictions. */
    private void processRestriction(long relationId, Relation relation) {
        long from = -1, to = -1, via = -1;
        for (Relation.Member member : relation.members) {
            if (member.role.equals("from")) {
                from = member.id;
            }
            else if (member.role.equals("to")) {
                to = member.id;
            }
            else if (member.role.equals("via")) {
                via = member.id;
            }
        }
        if (from == -1 || to == -1 || via == -1) {
            // FIXME clarify "bad" in error message (restriction was missing a role)
            LOG.warn(addBuilderAnnotation(new TurnRestrictionBad(relationId)));
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
                    LOG.debug(addBuilderAnnotation(new TurnRestrictionException(via, from)));
                }
            }
        }

        TurnRestrictionTag tag;
        if (relation.hasTag("restriction", "no_right_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.RIGHT);
        } else if (relation.hasTag("restriction", "no_left_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.LEFT);
        } else if (relation.hasTag("restriction", "no_straight_on")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.STRAIGHT);
        } else if (relation.hasTag("restriction", "no_u_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.U);
        } else if (relation.hasTag("restriction", "only_straight_on")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.STRAIGHT);
        } else if (relation.hasTag("restriction", "only_right_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.RIGHT);
        } else if (relation.hasTag("restriction", "only_left_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.LEFT);
        } else if (relation.hasTag("restriction", "only_u_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.U);
        } else {
            LOG.warn(addBuilderAnnotation(new TurnRestrictionUnknown(relation.getTag("restriction"))));
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
                LOG.info("Unparseable turn restriction: " + relationId);
            }
        }

        turnRestrictionsByFromWay.put(from, tag);
        turnRestrictionsByToWay.put(to, tag);
    }

    /**
     * Process an OSM level map. TODO what does "process" mean here?
     */
    private void processLevelMap(long relationId, Relation relation) {
        Map<String, OSMLevel> levels = OSMLevel.mapFromSpecList(relation.getTag("levels"), Source.LEVEL_MAP, true);
        for (Relation.Member member : relation.members) {
            if (member.type == Relation.Type.WAY && osm.ways.containsKey(member.id)) {
                Way way = osm.ways.get(member.id);
                if (way != null) {
                    // if the level map relation has a role:xyz tag, this way is something
                    // more complicated than a single level (e.g. ramp/stairway).
                    if (!relation.hasTag("role:" + member.role)) {
                        if (levels.containsKey(member.role)) {
                            wayLevels.put(member.id, levels.get(member.role));
                        } else {
                            LOG.warn(member.id + " has undefined level " + member.role);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle route=road relations. TODO what does "handle" mean here?
     * All ways that are members of these relations should inherit name and ref tags.
     */
    private void processRoad(long relationId, Relation relation) {
        for (Relation.Member member : relation.members) {
            if ( ! (member.type == Relation.Type.WAY && osm.ways.containsKey(member.id))) {
                continue;
            }

            Way way = osm.ways.get(member.id);
            if (way == null) {
                continue;
            }

            // FIXME why is this being done by modifying the tags on the source OSM data?
            // This is a text-based implementation of SetMultimap<Long, String>.
            /*
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
            */
        }
    }

    /**
     * Process an OSM public transport stop area relation.
     * 
     * This goes through all public_transport=stop_area relations and adds a mapping to stopsInAreas,
     * from the parent (either an area or multipolygon relation) to a a Set of transit stop nodes
     * that should be included in the parent area. TODO document: what happens when they are "included"?
     * This improves TransitToTaggedStopsGraphBuilder by enabling us to have unconnected stop nodes within the
     * areas by creating relations.
     * TODO document: what is an "unconnected stop node"? Why would we want to have them?
     *
     * @author hannesj
     * @see "http://wiki.openstreetmap.org/wiki/Tag:public_transport%3Dstop_area"
     */
    private void processPublicTransportStopArea(long relationId, Relation relation) {
        Tagged platformArea = null;
        Set<Long> platformNodes = Sets.newHashSet();
        for (Relation.Member member : relation.members) {
            if (member.type == Relation.Type.WAY && "platform".equals(member.role)
                    && areaWayIds.contains(member.id)) {
                if (platformArea == null) {
                    platformArea = osm.ways.get(member.id);
                } else
                    LOG.warn("Too many areas in relation " + relationId); {
                }
            }
            else if (member.type == Relation.Type.RELATION && "platform".equals(member.role)
                    && osm.relations.containsKey(member.id)) {
                if (platformArea == null) {
                    platformArea = osm.relations.get(member.id);
                } else {
                    LOG.warn("Too many areas in relation " + relationId);
                }
            }
            else if (member.type == Relation.Type.NODE && osm.nodes.containsKey(member.id)) {
                platformNodes.add(member.id);
            }
        }
        if (platformArea != null && !platformNodes.isEmpty()) {
            stopsInAreas.put(platformArea, platformNodes);
        } else {
            LOG.warn("Unable to process public transportation relation " + relationId);
        }
    }

    private String addBuilderAnnotation(GraphBuilderAnnotation annotation) {
        annotations.add(annotation);
        return annotation.getMessage();
    }
    
    /** Check if a point is within an epsilon of a node. */
    private static boolean checkIntersectionDistance(Point p, Node n, double epsilon) {
    	return Math.abs(p.getY() - n.getLat()) < epsilon && Math.abs(p.getX() - n.getLon()) < epsilon;
    }
    
    /** Check if two nodes are within an epsilon. */
    private static boolean checkDistance(Node a, Node b, double epsilon) {
    	return Math.abs(a.getLat() - b.getLat()) < epsilon && Math.abs(a.getLon()- b.getLon()) < epsilon;
	}
}
