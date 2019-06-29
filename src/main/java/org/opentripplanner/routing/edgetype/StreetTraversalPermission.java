package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Modes of travel that can traverse a street in a single direction.
 *
 * There are 4 types of Street Travel that are assumed here:
 * - PEDESTRIAN: this involves a person walking along a street. This mode is also used to model walking a bicycle or
 *  walking with a micromobility vehicle
 * - BICYCLE: this involves riding a bicycle and generally travelling at a faster speed than walking.
 * - CAR: driving a car at the fastest possible speed along a roadway
 * - MICROMOBILITY: riding a vehicle that weighs less than 500kg and has a motor. The distinction between this mode and
 *  bicycling is needed because some roads that do allow bicycling do not allow eScooters.
 *
 * Each StreetEdge is assigned a StreetTravelPermission that is used when traversing the edge to determine if the
 * current mode of travel in the shortest path search can be used to traverse the street. The above 4 modes can be
 * combined together to represent a composite set of permissions for a StreetEdge.
 */
public enum StreetTraversalPermission {
    NONE(0),
    PEDESTRIAN(1),
    BICYCLE(2),
    CAR(4),
    MICROMOBILITY(8),
    PEDESTRIAN_AND_BICYCLE(2 | 1),
    PEDESTRIAN_AND_CAR(4 | 1),
    BICYCLE_AND_CAR(4 | 2),
    PEDESTRIAN_AND_BICYCLE_AND_CAR(4 | 2 | 1),
    PEDESTRIAN_AND_MICROMOBILITY(8 | 1),
    BICYCLE_AND_MICROMOBILITY(8 | 2),
    PEDESTRIAN_AND_BICYCLE_AND_MICROMOBILITY(8 | 2 | 1),
    CAR_AND_MICROMOBILITY(8 | 4),
    BICYCLE_AND_CAR_AND_MICROMOBILITY(8 | 4 | 2),
    ALL(8 | 4 | 2 | 1);

    private static final Map<Integer, StreetTraversalPermission> lookup = new HashMap<Integer, StreetTraversalPermission>();

    static {
        for (StreetTraversalPermission s : EnumSet.allOf(StreetTraversalPermission.class))
            lookup.put(s.code, s);
    }

    public int code;

    private StreetTraversalPermission(int code) {
        this.code = code;
    }

    public static StreetTraversalPermission get(int code) {
        return lookup.get(code);
    }

    public StreetTraversalPermission add(StreetTraversalPermission perm) {
        return get(this.code | perm.code);
    }

    /**
     * Returns intersection of allowed permissions between current permissions and given permissions
     *
     * @param perm
     * @return
     */
    public StreetTraversalPermission intersection(StreetTraversalPermission perm) {
        return get(this.code & perm.code);
    }

    public StreetTraversalPermission remove(StreetTraversalPermission perm) {
        return get(this.code & ~perm.code);
    }

    public StreetTraversalPermission modify(boolean permissive, StreetTraversalPermission perm) {
        return permissive ? add(perm) : remove(perm);
    }

    public boolean allows(StreetTraversalPermission perm) {
        return (code & perm.code) != 0;
    }
    
    /**
     * Returns true if any of the specified modes are allowed to use this street.
     */
    public boolean allows(TraverseModeSet modes) {
        if (modes.getWalk() && allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        } else if (modes.getBicycle() && allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        } else if (modes.getMicromobility() && allows(StreetTraversalPermission.MICROMOBILITY)) {
            return true;
        } else if (modes.getCar() && allows(StreetTraversalPermission.CAR)) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the given mode is allowed to use this street.
     */
    public boolean allows(TraverseMode mode) {
        if (mode == TraverseMode.WALK && allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        } else if (mode == TraverseMode.BICYCLE && allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        } else if (mode == TraverseMode.MICROMOBILITY && allows(StreetTraversalPermission.MICROMOBILITY)) {
            return true;
        } else if (mode == TraverseMode.CAR && allows(StreetTraversalPermission.CAR)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if there are any modes allowed by this permission.
     */
    public boolean allowsAnything() {
        return !this.allowsNothing();
    }
    
    /**
     * Returns true if there no modes are by this permission.
     */
    public boolean allowsNothing() {
        // TODO(flamholz): what about CROSSHATCHED?
        return this == StreetTraversalPermission.NONE;
    }
}
