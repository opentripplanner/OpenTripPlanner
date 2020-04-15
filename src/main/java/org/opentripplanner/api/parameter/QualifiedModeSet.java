package org.opentripplanner.api.parameter;

import com.beust.jcommander.internal.Sets;

import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.request.RequestModes;
import org.opentripplanner.routing.request.StreetMode;

import java.io.Serializable;
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

    public RequestModes getRequestModes() {
        RequestModes requestModes = new RequestModes(
            StreetMode.WALK,
            StreetMode.WALK,
            StreetMode.WALK,
            new HashSet<>()
        );

        // Set transit modes
        for (QualifiedMode qMode : qModes) {
             switch (qMode.mode) {
                 case "RAIL":
                     requestModes.transitModes.add(TransitMode.RAIL);
                     break;
                 case "SUBWAY":
                     requestModes.transitModes.add(TransitMode.SUBWAY);
                     break;
                 case "BUS":
                     requestModes.transitModes.add(TransitMode.BUS);
                     break;
                 case "TRAM":
                     requestModes.transitModes.add(TransitMode.TRAM);
                     break;
                 case "FERRY":
                     requestModes.transitModes.add(TransitMode.FERRY);
                     break;
                 case "AIRPLANE":
                     requestModes.transitModes.add(TransitMode.AIRPLANE);
                     break;
                 case "CABLE_CAR":
                     requestModes.transitModes.add(TransitMode.CABLE_CAR);
                     break;
                 case "GONDOLA":
                     requestModes.transitModes.add(TransitMode.GONDOLA);
                     break;
                 case "FUNICULAR":
                     requestModes.transitModes.add(TransitMode.FUNICULAR);
                     break;
             }
        }

        //  This is a best effort at mapping QualifiedModes to access/egress/direct StreetModes.
        //  It was unclear what exactly each combination of QualifiedModes should mean.
        //  TODO OTP2 This should either be updated with missing modes or the REST API should be
        //   redesigned to better reflect the mode structure used in RequestModes.
        //   Also, some StreetModes are implied by combination of QualifiedModes and are not covered
        //   in this mapping.
        for (QualifiedMode qMode : qModes) {
            switch (qMode.mode) {
                case "WALK":
                    requestModes.accessMode = StreetMode.WALK;
                    requestModes.egressMode = StreetMode.WALK;
                    requestModes.directMode = StreetMode.WALK;
                    break;
                case "BICYCLE":
                    if (qMode.qualifiers.contains("RENT")) {
                        requestModes.accessMode = StreetMode.BIKE_RENTAL;
                        requestModes.egressMode = StreetMode.BIKE_RENTAL;
                        requestModes.directMode = StreetMode.BIKE_RENTAL;
                    }
                    else if (qMode.qualifiers.contains("PARK")) {
                        requestModes.accessMode = StreetMode.BIKE_TO_PARK;
                        requestModes.egressMode = StreetMode.WALK;
                        requestModes.directMode = StreetMode.BIKE_TO_PARK;
                    }
                    else {
                        requestModes.accessMode = StreetMode.BIKE;
                        requestModes.egressMode = StreetMode.BIKE;
                        requestModes.directMode = StreetMode.BIKE;
                    }
                    break;
                case "CAR":
                    if (qMode.qualifiers.contains("RENT")) {
                        requestModes.accessMode = StreetMode.CAR_RENTAL;
                        requestModes.egressMode = StreetMode.CAR_RENTAL;
                        requestModes.directMode = StreetMode.CAR_RENTAL;
                    }
                    else if (qMode.qualifiers.contains("PARK")) {
                        requestModes.accessMode = StreetMode.CAR_TO_PARK;
                        requestModes.egressMode = StreetMode.WALK;
                        requestModes.directMode = StreetMode.CAR_TO_PARK;
                    }
                    else {
                        requestModes.accessMode = StreetMode.WALK;
                        requestModes.egressMode = StreetMode.WALK;
                        requestModes.directMode = StreetMode.CAR;
                    }
                    break;
            }
        }

        return requestModes;
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
