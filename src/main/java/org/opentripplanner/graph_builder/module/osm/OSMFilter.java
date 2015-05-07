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

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.ConflictingBikeTags;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class OSMFilter {

    private static Logger LOG = LoggerFactory.getLogger(OSMFilter.class);

    /**
     * Determine whether any mode can or should ever traverse the given way. If not, we leave the
     * way out of the OTP graph. Potentially routable ways are those that have the tags : highway=*
     * public_transport=platform railway=platform
     * 
     * But not conveyers, proposed highways/roads or those still under construction, and raceways
     * (as well as ways where all access is specifically forbidden to the public).
     * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dproposed
     */
    public static boolean isWayRoutable(OSMWithTags way) {
        if (!isOsmEntityRoutable(way))
            return false;

        String highway = way.getTag("highway");
        if (highway != null
                && (highway.equals("conveyer") || highway.equals("proposed")
                        || highway.equals("construction") || highway.equals("raceway") || highway
                            .equals("unbuilt")))
            return false;

        if (way.isGeneralAccessDenied()) {
            // There are exceptions.
            return (way.isMotorcarExplicitlyAllowed() || way.isBicycleExplicitlyAllowed() || way
                    .isPedestrianExplicitlyAllowed() || way.isMotorVehicleExplicitlyAllowed());
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
    public static boolean isOsmEntityRoutable(OSMWithTags osmEntity) {
        if (osmEntity.hasTag("highway"))
            return true;
        if (osmEntity.isTag("public_transport", "platform")
                || osmEntity.isTag("railway", "platform")) {
            return !("tourism".equals(osmEntity.getTag("usage")));
        }
        return false;
    }

    public static StreetTraversalPermission getPermissionsForEntity(OSMWithTags entity,
            StreetTraversalPermission def) {
        StreetTraversalPermission permission = null;

        /*
         * Only a few tags are examined here, because we only care about modes supported by OTP
         * (wheelchairs are not of concern here)
         * 
         * Only a few values are checked for, all other values are presumed to be permissive (=>
         * This may not be perfect, but is closer to reality, since most people don't follow the
         * rules perfectly ;-)
         */
        if (entity.isGeneralAccessDenied()) {
            // this can actually be overridden
            permission = StreetTraversalPermission.NONE;
            if (entity.isMotorcarExplicitlyAllowed() || entity.isMotorVehicleExplicitlyAllowed()) {
                permission = permission.add(StreetTraversalPermission.CAR);
            }
            if (entity.isBicycleExplicitlyAllowed()) {
                permission = permission.add(StreetTraversalPermission.BICYCLE);
            }
            if (entity.isPedestrianExplicitlyAllowed()) {
                permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
            }
        } else {
            permission = def;
        }

        if (entity.isMotorcarExplicitlyDenied() || entity.isMotorVehicleExplicitlyDenied()) {
            permission = permission.remove(StreetTraversalPermission.CAR);
        } else if (entity.isMotorcarExplicitlyAllowed() || entity.isMotorVehicleExplicitlyAllowed()) {
            permission = permission.add(StreetTraversalPermission.CAR);
        }

        if (entity.isBicycleExplicitlyDenied()) {
            permission = permission.remove(StreetTraversalPermission.BICYCLE);
        } else if (entity.isBicycleExplicitlyAllowed()) {
            permission = permission.add(StreetTraversalPermission.BICYCLE);
        }

        if (entity.isPedestrianExplicitlyDenied()) {
            permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
        } else if (entity.isPedestrianExplicitlyAllowed()) {
            permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
        }

        if (entity.isUnderConstruction()) {
            permission = StreetTraversalPermission.NONE;
        }

        if (permission == null)
            return def;

        return permission;
    }

    /**
     * Computes permissions for an OSMWay.
     * 
     * @param way
     * @param def
     * @return
     */
    public static StreetTraversalPermission getPermissionsForWay(OSMWay way,
            StreetTraversalPermission def, Graph graph) {
        StreetTraversalPermission permissions = getPermissionsForEntity(way, def);

        /*
         * pedestrian rules: everything is two-way (assuming pedestrians are allowed at all) bicycle
         * rules: default: permissions;
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
        if (way.isPedestrianExplicitlyAllowed()) {
            permissions = permissions.add(StreetTraversalPermission.PEDESTRIAN);
        } else if (way.isPedestrianExplicitlyDenied()) {
            permissions = permissions.remove(StreetTraversalPermission.PEDESTRIAN);
        }

        // Compute bike permissions, check consistency.
        boolean forceBikes = false;
        if (way.isBicycleExplicitlyAllowed()) {
            permissions = permissions.add(StreetTraversalPermission.BICYCLE);
            forceBikes = true;
        }

        if (way.isBicycleDismountForced()) {
            permissions = permissions.remove(StreetTraversalPermission.BICYCLE);
            if (forceBikes) {
                LOG.warn(graph.addBuilderAnnotation(new ConflictingBikeTags(way.getId())));
            }
        }

        return permissions;
    }

    /**
     * Check OSM tags for various one-way and one-way-by-mode tags and return a pair of permissions
     * for travel along and against the way.
     */
    public static P2<StreetTraversalPermission> getPermissions(
            StreetTraversalPermission permissions, OSMWay way) {

        StreetTraversalPermission permissionsFront = permissions;
        StreetTraversalPermission permissionsBack = permissions;

        // Check driving direction restrictions.
        if (way.isOneWayForwardDriving() || way.isRoundabout()) {
            permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
        }
        if (way.isOneWayReverseDriving()) {
            permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
        }

        // Check bike direction restrictions.
        if (way.isOneWayForwardBicycle()) {
            permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
        }
        if (way.isOneWayReverseBicycle()) {
            permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
        }

        // TODO(flamholz): figure out what this is for.
        String oneWayBicycle = way.getTag("oneway:bicycle");
        if (OSMWithTags.isFalse(oneWayBicycle) || way.isTagTrue("bicycle:backwards")) {
            if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
                permissionsFront = permissionsFront.add(StreetTraversalPermission.BICYCLE);
                permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
            }
        }

        //This needs to be after adding permissions for oneway:bicycle=no
        //removes bicycle permission when bicycles need to use sidepath
        //TAG: bicycle:forward=use_sidepath
        if (way.isForwardDirectionSidepath()) {
            permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
        }

        //TAG bicycle:backward=use_sidepath
        if (way.isReverseDirectionSidepath()) {
            permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
        }

        if (way.isOpposableCycleway()) {
            permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
        }
        return new P2<StreetTraversalPermission>(permissionsFront, permissionsBack);
    }

    public static int getStreetClasses(OSMWithTags way) {
        int link = 0;
        String highway = way.getTag("highway");
        if (highway != null && highway.endsWith(("_link"))) {
            link = StreetEdge.CLASS_LINK;
        }
        return getPlatformClass(way) | link;
    }

    public static int getPlatformClass(OSMWithTags way) {
        String highway = way.getTag("highway");
        if ("platform".equals(way.getTag("railway"))) {
            return StreetEdge.CLASS_TRAIN_PLATFORM;
        }
        if ("platform".equals(highway) || "platform".equals(way.getTag("public_transport"))) {
            if (way.isTagTrue("train") || way.isTagTrue("subway") || way.isTagTrue("tram")
                    || way.isTagTrue("monorail")) {
                return StreetEdge.CLASS_TRAIN_PLATFORM;
            }
            return StreetEdge.CLASS_OTHER_PLATFORM;
        }
        return 0;
    }

}
