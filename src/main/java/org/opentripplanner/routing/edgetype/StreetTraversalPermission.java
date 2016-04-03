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

/**
 * Who can traverse a street in a single direction.
 */
public enum StreetTraversalPermission {
    NONE(0),
    PEDESTRIAN(1),
    BICYCLE(2),
    PEDESTRIAN_AND_BICYCLE(2 | 1),
    CAR(4),
    PEDESTRIAN_AND_CAR(4 | 1),
    BICYCLE_AND_CAR(4 | 2),
    ALL(4 | 2 | 1);

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
