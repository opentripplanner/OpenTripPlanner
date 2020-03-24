package org.opentripplanner.api.parameter;

import com.beust.jcommander.internal.Sets;

import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.request.AllowedModes;
import org.opentripplanner.routing.request.StreetMode;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

    public AllowedModes getAllowedModes() {
        AllowedModes allowedModes = new AllowedModes(
            StreetMode.WALK,
            StreetMode.WALK,
            StreetMode.WALK,
            Collections.emptySet()
        );

        if (qModes.isEmpty()) return allowedModes;

        // Set transit modes
        for (QualifiedMode qMode : qModes) {
             switch (qMode.mode) {
                 case "RAIL":
                     allowedModes.transitModes.add(TransitMode.RAIL);
                     break;
                 case "SUBWAY":
                     allowedModes.transitModes.add(TransitMode.SUBWAY);
                     break;
                 case "BUS":
                     allowedModes.transitModes.add(TransitMode.BUS);
                     break;
                 case "TRAM":
                     allowedModes.transitModes.add(TransitMode.TRAM);
                     break;
                 case "FERRY":
                     allowedModes.transitModes.add(TransitMode.FERRY);
                     break;
                 case "AIRPLANE":
                     allowedModes.transitModes.add(TransitMode.AIRPLANE);
                     break;
                 case "CABLE_CAR":
                     allowedModes.transitModes.add(TransitMode.CABLE_CAR);
                 case "GONDOLA":
                     allowedModes.transitModes.add(TransitMode.GONDOLA);
                     break;
                 case "FUNICULAR":
                     allowedModes.transitModes.add(TransitMode.FUNICULAR);
                     break;
             }
        }

        if (allowedModes.transitModes.isEmpty()) {
            allowedModes.transitModes = new HashSet<>(Arrays.asList(TransitMode.values()));
        }

        //  This is a best effort at mapping QualifiedModes to access/egress/direct StreetModes.
        //  It was unclear what exactly each combination of QualifiedModes should mean.
        //  TODO OTP2 This should either be updated with missing modes or the REST API should be
        //   redesigned to better reflect the mode structure used in AllowedModes.
        for (QualifiedMode qMode : qModes) {
            switch (qMode.mode) {
                case "TraverseMode.WALK":
                    allowedModes.accessMode = StreetMode.WALK;
                    allowedModes.egressMode = StreetMode.WALK;
                    allowedModes.directMode = StreetMode.WALK;
                    break;
                case "BICYCLE":
                    if (qMode.qualifiers.contains(Qualifier.RENT)) {
                        allowedModes.accessMode = StreetMode.BIKE_RENTAL;
                        allowedModes.egressMode = StreetMode.BIKE_RENTAL;
                        allowedModes.directMode = StreetMode.BIKE_RENTAL;
                    }
                    else if (qMode.qualifiers.contains(Qualifier.PARK)) {
                        allowedModes.accessMode = StreetMode.BIKE_TO_PARK;
                        allowedModes.egressMode = StreetMode.WALK;
                        allowedModes.directMode = StreetMode.BIKE_TO_PARK;
                    }
                    else {
                        allowedModes.accessMode = StreetMode.BIKE;
                        allowedModes.egressMode = StreetMode.BIKE;
                        allowedModes.directMode = StreetMode.BIKE;
                    }
                    break;
                case "TraverseMode.CAR":
                    if (qMode.qualifiers.contains(Qualifier.RENT)) {
                        allowedModes.accessMode = StreetMode.CAR_RENTAL;
                        allowedModes.egressMode = StreetMode.CAR_RENTAL;
                        allowedModes.directMode = StreetMode.CAR_RENTAL;
                    }
                    else if (qMode.qualifiers.contains(Qualifier.PARK)) {
                        allowedModes.accessMode = StreetMode.CAR_TO_PARK;
                        allowedModes.egressMode = StreetMode.WALK;
                        allowedModes.directMode = StreetMode.CAR_TO_PARK;
                    }
                    else {
                        allowedModes.accessMode = StreetMode.WALK;
                        allowedModes.egressMode = StreetMode.WALK;
                        allowedModes.directMode = StreetMode.CAR;
                    }
                    break;
            }
        }

        return allowedModes;
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
