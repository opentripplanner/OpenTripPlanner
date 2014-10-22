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

package org.opentripplanner.graph_builder.impl.osm;

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

import org.opentripplanner.common.RepeatingTimePeriod;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.LevelAmbiguous;
import org.opentripplanner.graph_builder.annotation.TurnRestrictionBad;
import org.opentripplanner.graph_builder.annotation.TurnRestrictionException;
import org.opentripplanner.graph_builder.annotation.TurnRestrictionUnknown;
import org.opentripplanner.graph_builder.impl.osm.TurnRestrictionTag.Direction;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMLevel.Source;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class OSMDatabase implements OpenStreetMapContentHandler {

    private static Logger LOG = LoggerFactory.getLogger(OSMDatabase.class);

    private Map<Long, OSMNode> _nodes = new HashMap<Long, OSMNode>();

    private HashSet<OSMNode> _bikeRentalNodes = new HashSet<OSMNode>();

    private Map<Long, OSMWay> _ways = new HashMap<Long, OSMWay>();

    private List<Area> _walkableAreas = new ArrayList<Area>();

    private List<Area> _parkAndRideAreas = new ArrayList<Area>();

    private Set<Long> _areaWayIds = new HashSet<Long>();

    private Map<Long, OSMWay> _areaWaysById = new HashMap<Long, OSMWay>();

    private Map<Long, Set<OSMWay>> _areasForNode = new HashMap<Long, Set<OSMWay>>();

    private List<OSMWay> _singleWayAreas = new ArrayList<OSMWay>();

    private Map<Long, OSMRelation> _relations = new HashMap<Long, OSMRelation>();

    private Set<OSMWithTags> _processedAreas = new HashSet<OSMWithTags>();

    private Set<Long> _nodesWithNeighbors = new HashSet<Long>();

    private Set<Long> _areaNodes = new HashSet<Long>();

    /* Track which vertical level each OSM way belongs to, for building elevators etc. */
    private Map<OSMWithTags, OSMLevel> wayLevels = new HashMap<OSMWithTags, OSMLevel>();

    private Multimap<Long, TurnRestrictionTag> turnRestrictionsByFromWay = ArrayListMultimap
            .create();

    private Multimap<Long, TurnRestrictionTag> turnRestrictionsByToWay = ArrayListMultimap.create();

    private Map<OSMWithTags, Set<OSMNode>> stopsInAreas = new HashMap<OSMWithTags, Set<OSMNode>>();

    private List<GraphBuilderAnnotation> annotations = new ArrayList<>();

    /**
     * If true, disallow zero floors and add 1 to non-negative numeric floors, as is generally done
     * in the United States. This does not affect floor names from level maps.
     */
    public boolean noZeroLevels = true;

    public OSMNode getNode(Long nodeId) {
        return _nodes.get(nodeId);
    }

    public Collection<OSMWay> getWays() {
        return Collections.unmodifiableCollection(_ways.values());
    }

    public Collection<OSMNode> getBikeRentalNodes() {
        return Collections.unmodifiableCollection(_bikeRentalNodes);
    }

    public Collection<Area> getWalkableAreas() {
        return Collections.unmodifiableCollection(_walkableAreas);
    }

    public Collection<Area> getParkAndRideAreas() {
        return Collections.unmodifiableCollection(_parkAndRideAreas);
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
        return wayLevels.get(way);
    }

    public boolean isNodeSharedByMultipleAreas(Long nodeId) {
        Set<OSMWay> areas = _areasForNode.get(nodeId);
        if (areas == null) {
            return false;
        }
        return areas.size() > 1;
    }

    public boolean isNodeBelongsToWay(Long nodeId) {
        return _nodesWithNeighbors.contains(nodeId);
    }

    public Collection<GraphBuilderAnnotation> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    @Override
    public void addNode(OSMNode node) {
        if (node.isTag("amenity", "bicycle_rental")) {
            _bikeRentalNodes.add(node);
            return;
        }
        if (!(_nodesWithNeighbors.contains(node.getId()) || _areaNodes.contains(node.getId()) || node
                .isStop()))
            return;

        if (_nodes.containsKey(node.getId()))
            return;

        _nodes.put(node.getId(), node);

        if (_nodes.size() % 100000 == 0)
            LOG.debug("nodes=" + _nodes.size());
    }

    @Override
    public void addWay(OSMWay way) {
        /* only add ways once */
        long wayId = way.getId();
        if (_ways.containsKey(wayId) || _areaWaysById.containsKey(wayId))
            return;

        if (_areaWayIds.contains(wayId)) {
            _areaWaysById.put(wayId, way);
        }

        getLevelsForWay(way);

        /* filter out ways that are not relevant for routing */
        if (!(OSMFilter.isWayRoutable(way) || way.isParkAndRide())) {
            return;
        }
        /* An area can be specified as such, or be one by default as an amenity */
        if ((way.isTag("area", "yes") || way.isTag("amenity", "parking"))
                && way.getNodeRefs().size() > 2) {
            // this is an area that's a simple polygon. So we can just add it straight
            // to the areas, if it's not part of a relation.
            if (!_areaWayIds.contains(wayId)) {
                _singleWayAreas.add(way);
                _areaWaysById.put(wayId, way);
                _areaWayIds.add(wayId);
                for (Long node : way.getNodeRefs()) {
                    MapUtils.addToMapSet(_areasForNode, node, way);
                }
            }
            return;
        }

        _ways.put(wayId, way);

        if (_ways.size() % 10000 == 0)
            LOG.debug("ways=" + _ways.size());
    }

    @Override
    public void addRelation(OSMRelation relation) {
        if (_relations.containsKey(relation.getId()))
            return;

        if (relation.isTag("type", "multipolygon") && OSMFilter.isOsmEntityRoutable(relation)) {
            // OSM MultiPolygons are ferociously complicated, and in fact cannot be processed
            // without reference to the ways that compose them. Accordingly, we will merely
            // mark the ways for preservation here, and deal with the details once we have
            // the ways loaded.
            if (!OSMFilter.isWayRoutable(relation) && !relation.isParkAndRide()) {
                return;
            }
            for (OSMRelationMember member : relation.getMembers()) {
                _areaWayIds.add(member.getRef());
            }
            getLevelsForWay(relation);
        } else if (!(relation.isTag("type", "restriction"))
                && !(relation.isTag("type", "route") && relation.isTag("route", "road"))
                && !(relation.isTag("type", "multipolygon") && OSMFilter
                        .isOsmEntityRoutable(relation))
                && !(relation.isTag("type", "level_map"))
                && !(relation.isTag("type", "public_transport") && relation.isTag(
                        "public_transport", "stop_area"))) {
            return;
        }

        _relations.put(relation.getId(), relation);

        if (_relations.size() % 100 == 0)
            LOG.debug("relations=" + _relations.size());

    }

    @Override
    public void doneFirstPhaseRelations() {
        // nothing to do here
    }

    @Override
    public void doneSecondPhaseWays() {
        // This copies relevant tags to the ways (highway=*) where it doesn't exist, so that
        // the way purging keeps the needed way around.
        // Multipolygons may be processed more than once, which may be needed since
        // some member might be in different files for the same multipolygon.

        // NOTE (AMB): this purging phase may not be necessary if highway tags are not
        // copied over from multipolygon relations. Perhaps we can get by with
        // only 2 steps -- ways+relations, followed by used nodes.
        // Ways can be tag-filtered in phase 1.

        markNodesForKeeping(_ways.values(), _nodesWithNeighbors);
        markNodesForKeeping(_areaWaysById.values(), _areaNodes);
    }

    /**
     * After all relations, ways, and nodes are loaded, handle areas.
     */
    @Override
    public void doneThirdPhaseNodes() {
        processMultipolygonRelations();
        AREA: for (OSMWay way : _singleWayAreas) {
            if (_processedAreas.contains(way)) {
                continue;
            }
            for (Long nodeRef : way.getNodeRefs()) {
                if (!_nodes.containsKey(nodeRef)) {
                    continue AREA;
                }
            }
            try {
                newArea(new Area(way, Arrays.asList(way), Collections.<OSMWay> emptyList(), _nodes));
            } catch (Area.AreaConstructionException e) {
                // this area cannot be constructed, but we already have all the
                // necessary nodes to construct it. So, something must be wrong with
                // the area; we'll mark it as processed so that we don't retry.
            }
            _processedAreas.add(way);
        }
    }

    /**
     * After all loading is done (from multiple OSM sources), post-process.
     */
    public void postLoad() {

        // handle turn restrictions, road names, and level maps in relations
        processRelations();

        // Remove all simple islands
        HashSet<Long> _keep = new HashSet<Long>(_nodesWithNeighbors);
        _keep.addAll(_areaNodes);
        _nodes.keySet().retainAll(_keep);
    }

    private void getLevelsForWay(OSMWithTags way) {
        /* Determine OSM level for each way, if it was not already set */
        if (!wayLevels.containsKey(way)) {
            // if this way is not a key in the wayLevels map, a level map was not
            // already applied in processRelations

            /* try to find a level name in tags */
            String levelName = null;
            OSMLevel level = OSMLevel.DEFAULT;
            if (way.hasTag("level")) { // TODO: floating-point levels &c.
                levelName = way.getTag("level");
                level = OSMLevel.fromString(levelName, OSMLevel.Source.LEVEL_TAG, noZeroLevels);
            } else if (way.hasTag("layer")) {
                levelName = way.getTag("layer");
                level = OSMLevel.fromString(levelName, OSMLevel.Source.LAYER_TAG, noZeroLevels);
            }
            if (level == null || (!level.reliable)) {
                LOG.warn(addBuilderAnnotation(new LevelAmbiguous(levelName, way.getId())));
                // TODO store a set of annotations?
                level = OSMLevel.DEFAULT;
            }
            wayLevels.put(way, level);
        }
    }

    private void markNodesForKeeping(Collection<OSMWay> osmWays, Set<Long> nodeSet) {
        for (Iterator<OSMWay> it = osmWays.iterator(); it.hasNext();) {
            OSMWay way = it.next();
            // Since the way is kept, update nodes-with-neighbors
            List<Long> nodes = way.getNodeRefs();
            if (nodes.size() > 1) {
                nodeSet.addAll(nodes);
            }
        }
    }

    /**
     * Copies useful metadata from multipolygon relations to the relevant ways, or to the area map.
     * This is done at a different time than processRelations(), so that way purging doesn't remove
     * the used ways.
     */
    private void processMultipolygonRelations() {
        RELATION: for (OSMRelation relation : _relations.values()) {
            if (_processedAreas.contains(relation)) {
                continue;
            }
            if (!(relation.isTag("type", "multipolygon") && (OSMFilter
                    .isOsmEntityRoutable(relation) || relation.isParkAndRide()))) {
                continue;
            }
            // Area multipolygons -- pedestrian plazas
            ArrayList<OSMWay> innerWays = new ArrayList<OSMWay>();
            ArrayList<OSMWay> outerWays = new ArrayList<OSMWay>();
            for (OSMRelationMember member : relation.getMembers()) {
                String role = member.getRole();
                OSMWay way = _areaWaysById.get(member.getRef());
                if (way == null) {
                    // relation includes way which does not exist in the data. Skip.
                    continue RELATION;
                }
                for (Long nodeId : way.getNodeRefs()) {
                    if (!_nodes.containsKey(nodeId)) {
                        // this area is missing some nodes, perhaps because it is on
                        // the edge of the region, so we will simply not route on it.
                        continue RELATION;
                    }
                    MapUtils.addToMapSet(_areasForNode, nodeId, way);
                }
                if (role.equals("inner")) {
                    innerWays.add(way);
                } else if (role.equals("outer")) {
                    outerWays.add(way);
                } else {
                    LOG.warn("Unexpected role " + role + " in multipolygon");
                }
            }
            _processedAreas.add(relation);
            try {
                newArea(new Area(relation, outerWays, innerWays, _nodes));
            } catch (Area.AreaConstructionException e) {
                continue;
            }

            for (OSMRelationMember member : relation.getMembers()) {
                // multipolygons for attribute mapping
                if (!("way".equals(member.getType()) && _ways.containsKey(member.getRef()))) {
                    continue;
                }

                OSMWithTags way = _ways.get(member.getRef());
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
     * 
     * @param area
     */
    private void newArea(Area area) {
        StreetTraversalPermission permissions = OSMFilter.getPermissionsForEntity(area.parent,
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        if (OSMFilter.isOsmEntityRoutable(area.parent)
                && permissions != StreetTraversalPermission.NONE) {
            _walkableAreas.add(area);
        }
        if (area.parent.isParkAndRide()) {
            _parkAndRideAreas.add(area);
        }
    }

    /**
     * Copies useful metadata from relations to the relevant ways/nodes.
     */
    private void processRelations() {
        LOG.debug("Processing relations...");

        for (OSMRelation relation : _relations.values()) {
            if (relation.isTag("type", "restriction")) {
                processRestriction(relation);
            } else if (relation.isTag("type", "level_map")) {
                processLevelMap(relation);
            } else if (relation.isTag("type", "route")) {
                processRoad(relation);
            } else if (relation.isTag("type", "public_transport")) {
                processPublicTransportStopArea(relation);
            }
        }
    }

    /**
     * Store turn restrictions.
     * 
     * @param relation
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
            LOG.warn(addBuilderAnnotation(new TurnRestrictionBad(relation.getId())));
            return;
        }

        TraverseModeSet modes = new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.CAR,
                TraverseMode.CUSTOM_MOTOR_VEHICLE);
        String exceptModes = relation.getTag("except");
        if (exceptModes != null) {
            for (String m : exceptModes.split(";")) {
                if (m.equals("motorcar")) {
                    modes.setDriving(false);
                } else if (m.equals("bicycle")) {
                    modes.setBicycle(false);
                    LOG.debug(addBuilderAnnotation(new TurnRestrictionException(via, from)));
                }
            }
        }

        TurnRestrictionTag tag;
        if (relation.isTag("restriction", "no_right_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.RIGHT);
        } else if (relation.isTag("restriction", "no_left_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.LEFT);
        } else if (relation.isTag("restriction", "no_straight_on")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.STRAIGHT);
        } else if (relation.isTag("restriction", "no_u_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.U);
        } else if (relation.isTag("restriction", "only_straight_on")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.STRAIGHT);
        } else if (relation.isTag("restriction", "only_right_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.RIGHT);
        } else if (relation.isTag("restriction", "only_left_turn")) {
            tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.LEFT);
        } else if (relation.isTag("restriction", "only_u_turn")) {
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
                LOG.info("Unparseable turn restriction: " + relation.getId());
            }
        }

        turnRestrictionsByFromWay.put(from, tag);
        turnRestrictionsByToWay.put(to, tag);
    }

    /**
     * Process an OSM level map.
     * 
     * @param relation
     */
    private void processLevelMap(OSMRelation relation) {
        Map<String, OSMLevel> levels = OSMLevel.mapFromSpecList(relation.getTag("levels"),
                Source.LEVEL_MAP, true);
        for (OSMRelationMember member : relation.getMembers()) {
            if ("way".equals(member.getType()) && _ways.containsKey(member.getRef())) {
                OSMWay way = _ways.get(member.getRef());
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
     * 
     * @param relation
     */
    private void processRoad(OSMRelation relation) {
        for (OSMRelationMember member : relation.getMembers()) {
            if (!("way".equals(member.getType()) && _ways.containsKey(member.getRef()))) {
                continue;
            }

            OSMWithTags way = _ways.get(member.getRef());
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
     * @param relation
     * @author hannesj
     * @see "http://wiki.openstreetmap.org/wiki/Tag:public_transport%3Dstop_area"
     */
    private void processPublicTransportStopArea(OSMRelation relation) {
        OSMWithTags platformArea = null;
        Set<OSMNode> platformsNodes = new HashSet<>();
        for (OSMRelationMember member : relation.getMembers()) {
            if ("way".equals(member.getType()) && "platform".equals(member.getRole())
                    && _areaWayIds.contains(member.getRef())) {
                if (platformArea == null)
                    platformArea = _areaWaysById.get(member.getRef());
                else
                    LOG.warn("Too many areas in relation " + relation.getId());
            } else if ("relation".equals(member.getType()) && "platform".equals(member.getRole())
                    && _relations.containsKey(member.getRef())) {
                if (platformArea == null)
                    platformArea = _relations.get(member.getRef());
                else
                    LOG.warn("Too many areas in relation " + relation.getId());
            } else if ("node".equals(member.getType()) && _nodes.containsKey(member.getRef())) {
                platformsNodes.add(_nodes.get(member.getRef()));
            }
        }
        if (platformArea != null && !platformsNodes.isEmpty())
            stopsInAreas.put(platformArea, platformsNodes);
        else
            LOG.warn("Unable to process public transportation relation " + relation.getId());
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

    private String addBuilderAnnotation(GraphBuilderAnnotation annotation) {
        annotations.add(annotation);
        return annotation.getMessage();
    }

}
