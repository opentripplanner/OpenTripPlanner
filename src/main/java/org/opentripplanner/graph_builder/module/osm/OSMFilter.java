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
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.ConflictingBikeTags;
import org.opentripplanner.osm.Node;
import org.opentripplanner.osm.Tagged;
import org.opentripplanner.osm.Way;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *  Lots of static functions to check whether certain tags are set.
 *  TODO rename this class to something library-ish (Tags?)
 */
public abstract class OSMFilter {

    private static Logger LOG = LoggerFactory.getLogger(OSMFilter.class);

    /** @return true if the given key is explicitly denied access to the given entity. */
    private static boolean keyExplicitlyDeniesAccess(Tagged entity, String key) {
        String tagValue = entity.getTag(key);
        return "no".equals(tagValue) || "license".equals(tagValue);
    }

    /** For bicycles, in addition to the usual access denial values, check requirement to use a separate sidepath. */
    private static boolean bicycleExplicitlyDenied (Tagged entity) {
        return keyExplicitlyDeniesAccess(entity, "bicycle") || "use_sidepath".equals(entity.getTag("bicycle"));
    }

    /** @return true if the given key is explicitly allowed access to the given entity. */
    public static boolean keyExplicitlyAllowsAccess(Tagged entity, String key) {
        if (entity.hasNoTags()) {
            return false;
        }
        if (entity.tagIsTrue(key)) {
            return true;
        }
        String value = entity.getTag(key);
        return ("designated".equalsIgnoreCase(value) || "official".equalsIgnoreCase(value)
                || "permissive".equalsIgnoreCase(value) || "unknown".equalsIgnoreCase(value));
    }

    /** @return true if access is generally denied to this element (potentially with exceptions). */
    public static boolean isGeneralAccessDenied(Tagged entity) {
        return keyExplicitlyDeniesAccess(entity, "access");
    }

    /**
     * Determine whether any mode can or should ever traverse the given way. If not, we leave the
     * way out of the OTP graph. Potentially routable ways are those that have the tags : highway=*
     * public_transport=platform railway=platform
     * 
     * But not conveyers, proposed highways/roads or those still under construction, and raceways
     * (as well as ways where all access is specifically forbidden to the public).
     * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dproposed
     *
     * TODO why is this separate from isOsmEntityRoutable? It's being applied to relations as well.
     */
    public static boolean isWayRoutable (Tagged way) {
        if (!isOsmEntityRoutable(way)) {
            return false;
        }
        String highway = way.getTag("highway");
        if (highway != null && (highway.equals("conveyer") || highway.equals("proposed")
            || highway.equals("construction") || highway.equals("raceway") || highway.equals("unbuilt"))) {
            return false;
        }
        if (isGeneralAccessDenied(way)) {
            // Check for exceptions to general denial of access
            return keyExplicitlyAllowsAccess(way, "motorcar")
                    || keyExplicitlyAllowsAccess(way, "bicycle")
                    || keyExplicitlyAllowsAccess(way, "pedestrian");
        }
        return true;
    }


    /**
     * Determines whether this OSM way is considered routable. The majority of routable ways are
     * those with a highway= tag (which includes everything from motorways to hiking trails).
     * Anything with a public_transport=platform or railway=platform tag is also considered routable
     * even if it doesn't have a highway tag. Platforms are however filtered out if they are marked
     * usage=tourism. This prevents miniature tourist railways like the one in Portland's Zoo from
     * receiving a better score and pulling search endpoints away from real transit stops.
     */
    public static boolean isOsmEntityRoutable(Tagged osmEntity) {
        if (osmEntity.hasTag("highway")) {
            return true;
        }
        if (osmEntity.hasTag("public_transport", "platform") || osmEntity.hasTag("railway", "platform")) {
            if (osmEntity.hasTag("usage", "tourism")) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /** @return true if this node / area is a park-and-ride lot. */
    public static boolean isParkAndRide(Tagged entity) {
        String parkingType = entity.getTag("parking");
        String parkAndRide = entity.getTag("park_ride");
        return entity.hasTag("amenity", "parking")
                && (parkingType != null && parkingType.contains("park_and_ride"))
                || (parkAndRide != null && !parkAndRide.equalsIgnoreCase("no"));
    }

    /** @return true if this node / area is a bike parking. */
    public static boolean isBikeParking (Tagged entity) {
        return entity.hasTag("amenity", "bicycle_parking")
                && !entity.hasTag("access", "private") && !entity.hasTag("access", "no");
    }

    /** @return True if this node is a bike rental station. */
    public static boolean isBikeRental(Tagged entity) {
        return entity.hasTag("amenity", "bicycle_rental");
    }

    public static boolean isUnderConstruction(Tagged entity) {
        return "construction".equals(entity.getTag("highway")) ||
                "construction".equals(entity.getTag("cycleway"));
    }

    /** Gets OTP street traversal permissions based on the tags in an OSM entity of any kind. */
    public static StreetTraversalPermission getPermissionsForEntity(Tagged entity, StreetTraversalPermission def) {
        StreetTraversalPermission permission = null;

        /*
         * Only a few keys are examined here, because we only care about modes supported by OTP
         * (wheelchairs are not of concern here)
         * 
         * Only a few values are checked for, all other values are presumed to be permissive.
         * This may not be perfect, but is closer to reality, since most people don't follow the
         * rules perfectly ;-)
         */
        if (isGeneralAccessDenied(entity)) {
            // this can actually be overridden
            permission = StreetTraversalPermission.NONE;
            if (keyExplicitlyAllowsAccess(entity, "motorcar")) {
                permission = permission.add(StreetTraversalPermission.CAR);
            }
            if (keyExplicitlyAllowsAccess(entity, "bicycle")) {
                permission = permission.add(StreetTraversalPermission.BICYCLE);
            }
            if (keyExplicitlyAllowsAccess(entity, "foot")) {
                permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
            }
        } else {
            permission = def;
        }

        if (keyExplicitlyDeniesAccess(entity, "motorcar")) {
            permission = permission.remove(StreetTraversalPermission.CAR);
        } else if (entity.hasTag("motorcar")) {
            permission = permission.add(StreetTraversalPermission.CAR);
        }

        if (bicycleExplicitlyDenied(entity)) {
            permission = permission.remove(StreetTraversalPermission.BICYCLE);
        } else if (entity.hasTag("bicycle")) {
            permission = permission.add(StreetTraversalPermission.BICYCLE);
        }

        if (keyExplicitlyDeniesAccess(entity, "foot")) {
            permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
        } else if (entity.hasTag("foot")) {
            permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
        }

        if (isUnderConstruction(entity)) {
            permission = StreetTraversalPermission.NONE;
        }

        if (permission == null) {
            return def;
        }

        return permission;
    }

    /**
     * Gets OTP street traversal permissions based on the tags in an OSM Way, building on the version of this function
     * for OSM entities of any kind.
     */
    public static StreetTraversalPermission getPermissionsForWay (long wayId, Way way,
            StreetTraversalPermission def, Graph graph) {
            // TODO passing the graph and way ID in just to register annotations is ugly

            StreetTraversalPermission permissions = getPermissionsForEntity(way, def);

        /*
         * pedestrian rules: everything is two-way (assuming pedestrians are allowed at all)
         * bicycle rules: default: permissions;
         * 
         * cycleway=dismount means walk your bike -- the engine will automatically try walking bikes
         * any time it is forbidden to ride them, so the only thing to do here is to remove bike
         * permissions
         * 
         * oneway=... sets permissions for cars and bikes oneway:bicycle overwrites these
         * permissions for bikes only
         * 
         * now, cycleway=opposite_lane, opposite, opposite_track can allow once oneway has been set
         * by oneway:bicycle, but should give a warning if it conflicts with oneway:bicycle
         * 
         * bicycle:backward=yes works like oneway:bicycle=no bicycle:backwards=no works like
         * oneway:bicycle=yes
         */

        // Compute pedestrian permissions.
        if (keyExplicitlyAllowsAccess(way, "foot")) {
            permissions = permissions.add(StreetTraversalPermission.PEDESTRIAN);
        }
        else if (keyExplicitlyDeniesAccess(way, "foot")) {
            permissions = permissions.remove(StreetTraversalPermission.PEDESTRIAN);
        }

        // Compute bike permissions and check consistency of the tags.
        boolean forceBikes = false;
        if (keyExplicitlyAllowsAccess(way, "bicycle")) {
            permissions = permissions.add(StreetTraversalPermission.BICYCLE);
            forceBikes = true;
        }

        if (way.hasTag("cycleway", "dismount") || "dismount".equals(way.getTag("bicycle"))) {
            permissions = permissions.remove(StreetTraversalPermission.BICYCLE);
            if (forceBikes) {
                LOG.warn(graph.addBuilderAnnotation(new ConflictingBikeTags(wayId)));
            }
        }

        return permissions;
    }

    /**
     * Check OSM tags for various one-way and one-way-by-mode tags and return a pair of permissions
     * for travel along and against the way.
     */
    public static P2<StreetTraversalPermission> getPermissions (StreetTraversalPermission permissions, Way way) {

        StreetTraversalPermission permissionsFront = permissions;
        StreetTraversalPermission permissionsBack = permissions;

        // Check driving direction restrictions.
        if (isOneWayForwardDriving(way) || isRoundabout(way)) {
            permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
        }
        if (isOneWayReverseDriving(way)) {
            permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
        }

        // Check bike direction restrictions.
        if (isOneWayForwardBicycle(way)) {
            permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
        }
        if (isOneWayReverseBicycle(way)) {
            permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
        }
        if (way.tagIsFalse("oneway:bicycle") || way.tagIsTrue("bicycle:backwards")) {
            if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
                permissionsFront = permissionsFront.add(StreetTraversalPermission.BICYCLE);
                permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
            }
        }

        // Remove bicycle permission where bicycles need to use a sidepath.
        // This needs to be done after adding permissions based on oneway:bicycle
        if (isForwardDirectionSidepath(way)) {
            permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
        }
        if (isReverseDirectionSidepath(way)) {
            permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
        }
        if (isOpposableCycleway(way)) {
            permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
        }
        return new P2<StreetTraversalPermission>(permissionsFront, permissionsBack);
    }

    public static int getStreetClasses(Tagged way) {
        int link = 0;
        String highway = way.getTag("highway");
        if (highway != null && highway.endsWith(("_link"))) {
            link = StreetEdge.CLASS_LINK;
        }
        return getPlatformClass(way) | link;
    }

    public static int getPlatformClass(Tagged way) {
        String highway = way.getTag("highway");
        if ("platform".equals(way.getTag("railway"))) {
            return StreetEdge.CLASS_TRAIN_PLATFORM;
        }
        if ("platform".equals(highway) || "platform".equals(way.getTag("public_transport"))) {
            if (way.tagIsTrue("train") || way.tagIsTrue("subway") || way.tagIsTrue("tram")
                    || way.tagIsTrue("monorail")) {
                return StreetEdge.CLASS_TRAIN_PLATFORM;
            }
            return StreetEdge.CLASS_OTHER_PLATFORM;
        }
        return 0;
    }

    /**
     * @return whether the node is a public transport stop that can be linked to a transit stop vertex later on
     * @author hannesj
     */
    public static boolean isStop(Tagged entity) {
        return "bus_stop".equals(entity.getTag("highway"))
                || "tram_stop".equals(entity.getTag("railway"))
                || "station".equals(entity.getTag("railway"))
                || "halt".equals(entity.getTag("railway"))
                || "bus_station".equals(entity.getTag("amenity"));
    }

    /** @return the capacity of the given node if defined, or 0. */
    public static int getCapacity(Node node) throws NumberFormatException {
        String capacity = node.getTag("capacity");
        if (capacity == null) {
            return 0;
        }
        return Integer.parseInt(capacity);
    }

    /** @return true if these are steps. */
    public static boolean isSteps(Tagged entity) {
        return "steps".equals(entity.getTag("highway"));
    }

    /** @return true if this is a roundabout. */
    public static boolean isRoundabout(Tagged entity) {
        return "roundabout".equals(entity.getTag("junction"));
    }

    /**
     * Returns a name-like value for an entity (if one exists).
     * The otp: namespaced tags are created by OpenStreetMapModule#processRelations(). We should probably stop doing
     * that.
     */
    public static String getAssumedName(Tagged entity) {

        String name;

        name = entity.getTag("name");
        if (name != null) {
            return name;
        }
        name = entity.getTag("otp:route_name");
        if (name != null) {
            return name;
        }
        name = entity.getTag("otp:gen_name");
        if (name != null) {
            return name;
        }
        name = entity.getTag("otp:route_ref");
        if (name != null) {
            return name;
        }
        name = entity.getTag("ref");
        if (name != null) {
            return name;
        }
        return null;

    }

    /**
     * Currently returns true only for elevators. TODO inline this?
     * @return whether the node is multi-level one that should be decomposed to multiple coincident nodes.
     */
    public static boolean isMultiLevel(Node node) {
        return "elevator".equals(node.getTag("highway"));
    }

    /** TODO inline this? */
    public static boolean hasTrafficLight(Node node) {
        return "traffic_signals".equals(node.getTag("highway"));
    }

    /** @return true if through traffic is not allowed. */
    public static boolean throughTrafficDisallowed(Tagged entity) {
        String access = entity.getTag("access");
        return "destination".equals(access) || "private".equals(access)
                || "customers".equals(access) || "delivery".equals(access)
                || "forestry".equals(access) || "agricultural".equals(access);
    }


    /** @return true if this is a one-way street for driving. */
    public static boolean isOneWayForwardDriving(Tagged entity) {
        return entity.tagIsTrue("oneway");
    }

    /** @return true if this way is one-way in the opposite direction of its definition. */
    public static boolean isOneWayReverseDriving(Tagged entity) {
        return entity.hasTag("oneway", "-1");
    }

    /** @return true if bikes can only go forward. */
    public static boolean isOneWayForwardBicycle(Tagged entity) {
        return entity.tagIsTrue("oneway:bicycle") || entity.tagIsFalse("bicycle:backwards");
    }

    /** @return true if bikes can only go in the reverse direction. */
    public static boolean isOneWayReverseBicycle(Tagged entity) {
        return entity.hasTag("oneway:bicycle", "-1");
    }

    /** @return true if bikes must use a separate sidepath in the forward direction of travel. */
    public static boolean isForwardDirectionSidepath(Tagged entity) {
        return entity.hasTag("bicycle:forward", "use_sidepath");
    }

    /** @return true if bikes must use a separate sidepath in the reverse direction of travel. */
    public static boolean isReverseDirectionSidepath(Tagged entity) {
        return entity.hasTag("bicycle:backward", "use_sidepath");
    }

    /** @return whether there is a contraflow cycle path here. */
    public static boolean isOpposableCycleway(Tagged entity) {
        // any cycleway which is opposite* allows contraflow biking
        String cycleway = entity.getTag("cycleway");
        String cyclewayLeft = entity.getTag("cycleway:left");
        String cyclewayRight = entity.getTag("cycleway:right");
        return (cycleway != null && cycleway.startsWith("opposite"))
                || (cyclewayLeft != null && cyclewayLeft.startsWith("opposite"))
                || (cyclewayRight != null && cyclewayRight.startsWith("opposite"));
    }

    /** @return a map of key-value pairs for all tags whose keys begin with the given prefix and a colon. */
    public static Map<String, String> getTagsByPrefix (Tagged entity, String prefix) {
        Map<String, String> out = Maps.newHashMap();
        for (Tagged.Tag tag : entity.tags) {
            if (tag.key.equals(prefix) || tag.key.startsWith(prefix + ":")) {
                out.put(tag.key, tag.value);
            }
        }
        if (out.isEmpty()) {
            return null;
        }
        return out;
    }

}

