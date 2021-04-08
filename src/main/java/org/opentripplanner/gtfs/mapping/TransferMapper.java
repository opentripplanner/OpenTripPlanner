package org.opentripplanner.gtfs.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferType;

/** Responsible for mapping GTFS Transfer into the OTP model. */
class TransferMapper {
    private final RouteMapper routeMapper;

    private final StationMapper stationMapper;

    private final StopMapper stopMapper;

    private final TripMapper tripMapper;

    TransferMapper(
            RouteMapper routeMapper,
            StationMapper stationMapper,
            StopMapper stopMapper,
            TripMapper tripMapper
    ) {
        this.routeMapper = routeMapper;
        this.stationMapper = stationMapper;
        this.stopMapper = stopMapper;
        this.tripMapper = tripMapper;
    }

    Collection<Transfer> map(Collection<org.onebusaway.gtfs.model.Transfer> allTransfers) {
        return allTransfers.stream().flatMap(t -> this.map(t).stream()).collect(Collectors.toList());
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */

    Collection<Transfer> map(org.onebusaway.gtfs.model.Transfer orginal) {
        return orginal == null ? null : doMap(orginal);
    }

    private Collection<Transfer> doMap(org.onebusaway.gtfs.model.Transfer rhs) {

        Trip fromTrip = tripMapper.map(rhs.getFromTrip());
        Trip toTrip = tripMapper.map(rhs.getToTrip());
        Route fromRoute = routeMapper.map(rhs.getFromRoute());
        Route toRoute = routeMapper.map(rhs.getToRoute());
        TransferType transferType = TransferType.valueOfGtfsCode(rhs.getTransferType());
        int transferTime = rhs.getMinTransferTime();

        // Transfers may be specified using parent stations
        // (https://developers.google.com/transit/gtfs/reference/transfers-file)
        // "If the stop ID refers to a station that contains multiple stops, this transfer rule
        // applies to all stops in that station." we thus expand transfers that use parent stations
        // to all the member stops.

        Collection<Stop> fromStops = getStopOrChildStops(rhs.getFromStop());
        Collection<Stop> toStops = getStopOrChildStops(rhs.getToStop());

        Collection<Transfer> lhs = new ArrayList<>();

        for (Stop fromStop : fromStops) {
            for (Stop toStop : toStops ) {
                lhs.add(
                        new Transfer(
                                fromStop,
                                toStop,
                                fromRoute,
                                toRoute,
                                fromTrip,
                                toTrip,
                                transferType,
                                transferTime
                        ));
            }
        }

        return lhs;
    }

    private Collection<Stop> getStopOrChildStops(org.onebusaway.gtfs.model.Stop gtfsStop) {
        if (gtfsStop.getLocationType() == 0) {
            return Collections.singletonList(stopMapper.map(gtfsStop));
        } else {
            return stationMapper.map(gtfsStop).getChildStops();
        }
    }
}
