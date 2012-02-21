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
