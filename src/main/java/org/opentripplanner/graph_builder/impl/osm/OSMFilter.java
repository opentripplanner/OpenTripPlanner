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

import org.opentripplanner.graph_builder.annotation.ConflictingBikeTags;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 *
 */
public class OSMFilter {

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
                    .isPedestrianExplicitlyAllowed());
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
            if (entity.isMotorcarExplicitlyAllowed()) {
                permission = permission.add(StreetTraversalPermission.ALL_DRIVING);
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

        if (entity.isMotorcarExplicitlyDenied()) {
            permission = permission.remove(StreetTraversalPermission.ALL_DRIVING);
        } else if (entity.hasTag("motorcar")) {
            permission = permission.add(StreetTraversalPermission.ALL_DRIVING);
        }

        if (entity.isBicycleExplicitlyDenied()) {
            permission = permission.remove(StreetTraversalPermission.BICYCLE);
        } else if (entity.hasTag("bicycle")) {
            permission = permission.add(StreetTraversalPermission.BICYCLE);
        }

        if (entity.isPedestrianExplicitlyDenied()) {
            permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
        } else if (entity.hasTag("foot")) {
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
    public static StreetTraversalPermission getPermissionsForWay(OSMWay way, StreetTraversalPermission def) {
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
                // TODO
                //LOG.warn(graph.addBuilderAnnotation(new ConflictingBikeTags(way.getId())));
            }
        }

        return permissions;
    }

}
