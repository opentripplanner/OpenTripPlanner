/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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
package org.opentripplanner.routing.core;

import java.util.HashSet;

public enum TransportationMode {
    TRAM, SUBWAY, RAIL, BUS, FERRY, CABLE_CAR, GONDOLA, FUNICULAR, WALK, BICYCLE, BOARDING, ALIGHTING, TRANSFER;

    public static HashSet<TransportationMode> transitModes;
    static {
        transitModes = new HashSet<TransportationMode>();
        transitModes.add(TRAM);
        transitModes.add(SUBWAY);
        transitModes.add(RAIL);
        transitModes.add(BUS);
        transitModes.add(FERRY);
        transitModes.add(CABLE_CAR);
        transitModes.add(GONDOLA);
        transitModes.add(FUNICULAR);
    }
    
    public String toString() {
        return name().toLowerCase(); 
    }

    public boolean isTransitMode() {
        return transitModes.contains(this);
    }
}
