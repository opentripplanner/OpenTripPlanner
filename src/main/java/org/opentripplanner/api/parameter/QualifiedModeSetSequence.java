package org.opentripplanner.api.parameter;

import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

/**
 * An ordered list of sets of qualified modes. For example, if someone was in possession of a car
 * and wanted to park it and/or walk before taking a train or a tram, and finally rent a bicycle to
 * reach the destination: CAR_HAVE_PARK,WALK;TRAIN,TRAM;BIKE_RENT
 * It might also make sense to allow slashes meaning "or", or simply the word "or".
 *
 * This class and QualifiedMode are clearly somewhat inefficient and allow nonsensical combinations like
 * renting and parking a subway. They are not intended for use in routing. Rather, they simply parse the
 * language of mode specifications that may be given in the mode query parameter. They are then converted
 * into more efficient and useful representation in the routing request.
 */
public class QualifiedModeSetSequence {

    public List<Set<QualifiedMode>> sets = Lists.newArrayList();

    public QualifiedModeSetSequence(String s) {
        for (String seg : s.split(";")) {
            Set<QualifiedMode> qModeSet = Sets.newHashSet();
            for (String qMode : seg.split(",")) {
                qModeSet.add(new QualifiedMode(qMode));
            }
            if (!qModeSet.isEmpty()) {
                sets.add(qModeSet);
            }

        }
    }

    /**
     * Modify an existing routing request, setting fields to reflect these qualified modes.
     * This is intended as a temporary solution, and uses the current system of a single mode set,
     * accompanied by some flags to help with routing.
     */
    public void applyToRequest(RoutingRequest req) {
        /* Start with an empty mode set. */
        TraverseModeSet modes = new TraverseModeSet();
        /* Use only the first set of qualified modes for now. */
        if (sets.isEmpty()) return;
        Set<QualifiedMode> qModes = sets.get(0);
        // First, copy over all the modes
        for (QualifiedMode qMode : qModes) {
            modes.setMode(qMode.mode, true);
        }

        // We used to always set WALK to true, but this forced walking when someone wanted to use a bike.
        // We also want it to be possible to force biking-only (e.g. this is done in some consistency tests).
        // TODO clearly define mode semantics: does presence of mode mean it is allowable, preferred... ?

        for (QualifiedMode qMode : qModes) {
            if (qMode.mode == TraverseMode.BICYCLE) {
                if (qMode.qualifiers.contains(Qualifier.RENT)) {
                    modes.setMode(TraverseMode.WALK, true); // turn on WALK for bike rental mode
                    req.allowBikeRental = true;
                }
                if (modes.isTransit()) { // this is ugly, using both kinds of modeset at once
                    req.bikeParkAndRide = qMode.qualifiers.contains(Qualifier.PARK);
                }
            }
            if (qMode.mode == TraverseMode.CAR && modes.isTransit()) { // this is ugly, using both kinds of modeset at once
                if (qMode.qualifiers.contains(Qualifier.PARK)) {
                    req.parkAndRide = true;
                } else {
                    req.kissAndRide = true;
                }
            }
        }
        req.setModes(modes);
    }

}
