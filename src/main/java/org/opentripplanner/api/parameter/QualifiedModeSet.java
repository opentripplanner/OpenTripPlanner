package org.opentripplanner.api.parameter;

import com.beust.jcommander.internal.Sets;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.io.Serializable;
import java.util.Set;

/**
 * A set of qualified modes. The original intent was to allow a sequence of mode sets, but the shift to "long distance
 * mode" routing means that it will make more sense to specify access, egress, and transit modes in separate parameters. 
 * So now this only contains one mode set rather than a sequence of them.
 *  
 * This class and QualifiedMode are clearly somewhat inefficient and allow nonsensical combinations like
 * renting and parking a subway. They are not intended for use in routing. Rather, they simply parse the
 * language of mode specifications that may be given in the mode query parameter. They are then converted
 * into more efficient and useful representation in the routing request.
 */
public class QualifiedModeSet implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public Set<QualifiedMode> qModes = Sets.newHashSet();

    public QualifiedModeSet(String s) {
        for (String qMode : s.split(",")) {
            qModes.add(new QualifiedMode(qMode));
        }
    }

    /**
     * Modify an existing routing request, setting fields to reflect these qualified modes.
     * This is intended as a temporary solution, and uses the current system of a single mode set,
     * accompanied by some flags to help with routing.
     */
    public void applyToRoutingRequest(RoutingRequest req) {

        if (qModes.isEmpty()) return;

        /* Start with an empty mode set. */
        TraverseModeSet modes = new TraverseModeSet();
        req.setModes(modes);
        
        /* First, copy over all the unqualified modes and see if we are using transit. FIXME HACK */
        for (QualifiedMode qMode : qModes) {
            modes.setMode(qMode.mode, true);
        }
        boolean usingTransit = modes.isTransit();
        
        // We used to always set WALK to true, but this forced walking when someone wanted to use a bike.
        // We also want it to be possible to force biking-only (e.g. this is done in some consistency tests).
        // TODO clearly define mode semantics: does presence of mode mean it is allowable, preferred... ?

        for (QualifiedMode qMode : qModes) {
            qMode.applyToRoutingRequest(req, usingTransit);
        }
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (QualifiedMode qm : qModes) {
            sb.append(qm.toString());
            sb.append(" ");
        }
        return sb.toString();
    }

}
