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

public enum StreetTraversalPermission {
    NONE(0),

    PEDESTRIAN(1),
    BICYCLE(2),
    PEDESTRIAN_AND_BICYCLE(3),
    CAR(4),
    PEDESTRIAN_AND_CAR(5),
    BICYCLE_AND_CAR(6),
    ALL(7),
    CROSSHATCHED(15); //this street exists in both Beszel and Ul Qoma; traffic direction may depend on which city you're in.

    private static final Map<Integer,StreetTraversalPermission> lookup = new HashMap<Integer,StreetTraversalPermission>();

    static {
        for(StreetTraversalPermission s : EnumSet.allOf(StreetTraversalPermission.class))
            lookup.put(s.getCode(), s);
    }

    private int code;

    private StreetTraversalPermission(int code) {
        this.code = code;
    }

    public int getCode() { return code; }

    public static StreetTraversalPermission get(int code) {
        return lookup.get(code);
    }

    public StreetTraversalPermission add(StreetTraversalPermission perm) {
        return get(this.getCode() | perm.getCode());
    }

    public StreetTraversalPermission remove(StreetTraversalPermission perm) {
        return (this.getCode() & perm.getCode()) != 0 ?  get(this.getCode() ^ perm.getCode()) : this;
    }

    public StreetTraversalPermission modify(boolean permissive, StreetTraversalPermission perm) {
        return permissive ? add(perm) : remove(perm);
    }

    public boolean allows(StreetTraversalPermission perm) {
        return (this.getCode() & perm.getCode()) != 0;
    }
}
