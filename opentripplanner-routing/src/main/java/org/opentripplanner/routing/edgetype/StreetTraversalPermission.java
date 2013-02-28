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

package org.opentripplanner.routing.edgetype;

import java.util.Map;
import java.util.HashMap;
import java.util.EnumSet;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

import lombok.Getter;

/**
 * Who can traverse a street in a single direction.
 * 
 */
public enum StreetTraversalPermission {
    NONE(0),
    PEDESTRIAN(1),
    BICYCLE(2),
    PEDESTRIAN_AND_BICYCLE(2 | 1),
    CAR(4),
    PEDESTRIAN_AND_CAR(4 | 1),
    BICYCLE_AND_CAR(4 | 2),
    // This is a configurable motor vehicle that is not a vanilla car.
    // e.g. truck, motor bike, etc.
    CUSTOM_MOTOR_VEHICLE(8),
    ALL_DRIVING(8 | 4),
    ALL(8 | 4 | 2 | 1),
    CROSSHATCHED(16); // this street exists in both Beszel and Ul Qoma; traffic direction may depend on which city you're in.

    private static final Map<Integer, StreetTraversalPermission> lookup = new HashMap<Integer, StreetTraversalPermission>();

    static {
        for (StreetTraversalPermission s : EnumSet.allOf(StreetTraversalPermission.class))
            lookup.put(s.getCode(), s);
    }

    @Getter
    private int code;

    private StreetTraversalPermission(int code) {
        this.code = code;
    }

    public static StreetTraversalPermission get(int code) {
        return lookup.get(code);
    }

    public StreetTraversalPermission add(StreetTraversalPermission perm) {
        return get(this.getCode() | perm.getCode());
    }

    public StreetTraversalPermission remove(StreetTraversalPermission perm) {
        return get(this.getCode() & ~perm.getCode());
    }

    public StreetTraversalPermission modify(boolean permissive, StreetTraversalPermission perm) {
        return permissive ? add(perm) : remove(perm);
    }

    public boolean allows(StreetTraversalPermission perm) {
        return (code & perm.code) != 0;
    }
    
    /**
     * Returns true if any of these modes are allowed.
     * @param modes
     * @return
     */
    public boolean allows(TraverseModeSet modes) {
        if (modes.getWalk() && allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        } else if (modes.getBicycle() && allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        } else if (modes.getCar() && allows(StreetTraversalPermission.CAR)) {
            return true;
        } else if (modes.getCustomMotorVehicle()
                && allows(StreetTraversalPermission.CUSTOM_MOTOR_VEHICLE)) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if this mode is allowed.
     * @param mode
     * @return
     */
    public boolean allows(TraverseMode mode) {
        if (mode == TraverseMode.WALK && allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        } else if (mode == TraverseMode.BICYCLE && allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        } else if (mode == TraverseMode.CAR && allows(StreetTraversalPermission.CAR)) {
            return true;
        } else if (mode == TraverseMode.CUSTOM_MOTOR_VEHICLE
                && allows(StreetTraversalPermission.CUSTOM_MOTOR_VEHICLE)) {
            return true;
        }
        return false;
    }
}
