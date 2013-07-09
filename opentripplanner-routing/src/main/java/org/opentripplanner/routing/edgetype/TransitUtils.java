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
        switch (type) {
        case 1:
            return true;
        case 2:
            s1.setExtension("boardAlightRule", "mustPhone");
            break;
        case 3:
            s1.setExtension("boardAlightRule", "coordinateWithDriver");
            break;
        default:
            s1.setExtension("boardAlightRule", null);
            break;
        }
        return false;
    }

}
