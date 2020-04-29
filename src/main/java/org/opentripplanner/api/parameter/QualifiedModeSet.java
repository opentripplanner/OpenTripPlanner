package org.opentripplanner.api.parameter;

import com.beust.jcommander.internal.Sets;

import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

import java.io.Serializable;
import java.util.Arrays;
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
        StreetMode accessMode = null;
        StreetMode egressMode = null;
        StreetMode directMode = null;
        Set<TransitMode> transitModes = new HashSet<>();

        // Set transit modes
        for (QualifiedMode qMode : qModes) {
             switch (qMode.mode) {
                 case TRANSIT:
                     transitModes.addAll(Arrays.asList(TransitMode.values()));
                 case RAIL:
                     transitModes.add(TransitMode.RAIL);
                     break;
                 case SUBWAY:
                     transitModes.add(TransitMode.SUBWAY);
                     break;
                 case BUS:
                     transitModes.add(TransitMode.BUS);
                     break;
                 case TRAM:
                     transitModes.add(TransitMode.TRAM);
                     break;
                 case FERRY:
                     transitModes.add(TransitMode.FERRY);
                     break;
                 case AIRPLANE:
                     transitModes.add(TransitMode.AIRPLANE);
                     break;
                 case CABLE_CAR:
                     transitModes.add(TransitMode.CABLE_CAR);
                     break;
                 case GONDOLA:
                     transitModes.add(TransitMode.GONDOLA);
                     break;
                 case FUNICULAR:
                     transitModes.add(TransitMode.FUNICULAR);
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
                case WALK:
                    accessMode = StreetMode.WALK;
                    egressMode = StreetMode.WALK;
                    directMode = StreetMode.WALK;
                    break;
                case BICYCLE:
                    if (qMode.qualifiers.contains(Qualifier.RENT)) {
                        accessMode = StreetMode.BIKE_RENTAL;
                        egressMode = StreetMode.BIKE_RENTAL;
                        directMode = StreetMode.BIKE_RENTAL;
                    }
                    else if (qMode.qualifiers.contains(Qualifier.PARK)) {
                        accessMode = StreetMode.BIKE_TO_PARK;
                        egressMode = StreetMode.WALK;
                        directMode = StreetMode.BIKE_TO_PARK;
                    }
                    else {
                        accessMode = StreetMode.BIKE;
                        egressMode = StreetMode.BIKE;
                        directMode = StreetMode.BIKE;
                    }
                    break;
                case CAR:
                    if (qMode.qualifiers.contains(Qualifier.RENT)) {
                        accessMode = StreetMode.CAR_RENTAL;
                        egressMode = StreetMode.CAR_RENTAL;
                        directMode = StreetMode.CAR_RENTAL;
                    }
                    else if (qMode.qualifiers.contains(Qualifier.PARK)) {
                        accessMode = StreetMode.CAR_TO_PARK;
                        egressMode = StreetMode.WALK;
                        directMode = StreetMode.CAR_TO_PARK;
                    }
                    else {
                        accessMode = StreetMode.WALK;
                        egressMode = StreetMode.WALK;
                        directMode = StreetMode.CAR;
                    }
                    break;
            }
        }

        RequestModes requestModes = new RequestModes(
            accessMode,
            egressMode,
            directMode,
            transitModes
        );

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
