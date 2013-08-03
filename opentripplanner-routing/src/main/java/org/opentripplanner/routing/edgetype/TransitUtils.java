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

import org.opentripplanner.routing.core.StateEditor;

public class TransitUtils {

    /**  Returns true if this vehicle cannot be boarded/alighted from at this stop;
     * also makes notes in the state if there are special rules about boarding/alighting 
     */
    public static boolean handleBoardAlightType(StateEditor s1, int type) {
        Object boardAlightType = determineBoardAlightType(type);

        if (boardAlightType instanceof Boolean) return (Boolean) boardAlightType;

        s1.setExtension("boardAlightRule", boardAlightType);
        return false;
    }

    /**
     * Returns true if this vehicle can't be boarded/alighted from at this stop, false if it can be.
     * If there are special rules about boarding/alighting, a String describing them is returned.
     *
     * @param type The pattern-dependent board or alight type code
     * @return A String describing special rules about boarding/alighting if present, else a Boolean
     */
    public static Object determineBoardAlightType(int type) {
        switch (type) {
        default:
            return false;
        case 1:
            return true;
        case 2:
            return "mustPhone";
        case 3:
            return "coordinateWithDriver";
        }
    }
}
