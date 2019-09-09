package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.TransferType;
import org.opentripplanner.model.Trip;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Responsible for mapping GTFS Transfer into the OTP model. */
class TransferMapper {
    private final RouteMapper routeMapper;

    private final StationMapper stationMapper;

    private final StopMapper stopMapper;

    private final TripMapper tripMapper;

    private Map<org.onebusaway.gtfs.model.Transfer, Collection<Transfer>> mappedTransfers
            = new HashMap<>();

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
        return orginal == null ? null : mappedTransfers.computeIfAbsent(orginal, this::doMap);
    }

    // TODO This now returns a collection, which means the above methods need to be changed
    private Collection<Transfer> doMap(org.onebusaway.gtfs.model.Transfer rhs) {
        Collection<Transfer> lhs;

        Trip fromTrip = tripMapper.map(rhs.getFromTrip());
        Trip toTrip = tripMapper.map(rhs.getToTrip());
        Route fromRoute = routeMapper.map(rhs.getFromRoute());
        Route toRoute = routeMapper.map(rhs.getToRoute());
        TransferType transferType = mapTransferType(rhs.getTransferType());
        int transferTime = rhs.getMinTransferTime();

        // Transfers may be specified using parent stations
        // (https://developers.google.com/transit/gtfs/reference/transfers-file)
        // "If the stop ID refers to a station that contains multiple stops, this transfer rule
        // applies to all stops in that station." we thus expand transfers that use parent stations
        // to all the member stops.

        if (rhs.getFromStop().getLocationType() == 0
                && rhs.getToStop().getLocationType() == 0) {
            lhs = Transfer.getExpandedTransfers(
                    stopMapper.map(rhs.getFromStop()),
                    stopMapper.map(rhs.getToStop()),
                    fromRoute,
                    toRoute,
                    fromTrip,
                    toTrip,
                    transferType,
                    transferTime
            );
        } else if (rhs.getFromStop().getLocationType() == 1
                && rhs.getToStop().getLocationType() == 0) {
            lhs = Transfer.getExpandedTransfers(
                    stationMapper.map(rhs.getFromStop()),
                    stopMapper.map(rhs.getToStop()),
                    fromRoute,
                    toRoute,
                    fromTrip,
                    toTrip,
                    transferType,
                    transferTime
            );
        } else if (rhs.getFromStop().getLocationType() == 0
                && rhs.getToStop().getLocationType() == 1) {
            lhs = Transfer.getExpandedTransfers(
                    stopMapper.map(rhs.getFromStop()),
                    stationMapper.map(rhs.getToStop()),
                    fromRoute,
                    toRoute,
                    fromTrip,
                    toTrip,
                    transferType,
                    transferTime
            );
        } else if (rhs.getFromStop().getLocationType() == 1
                && rhs.getToStop().getLocationType() == 1) {
            lhs = Transfer.getExpandedTransfers(
                    stationMapper.map(rhs.getFromStop()),
                    stationMapper.map(rhs.getToStop()),
                    fromRoute,
                    toRoute,
                    fromTrip,
                    toTrip,
                    transferType,
                    transferTime
            );
        } else {
            lhs = Collections.emptyList();
        }
        return lhs;
    }

    private TransferType mapTransferType(int transferType) {
        switch (transferType) {
        case 3:
            return TransferType.FORBIDDEN;
        case 2:
            return TransferType.GUARANTEED_WITH_MIN_TIME;
        case 1:
            return TransferType.GUARANTEED;
        default:
        case 0:
            return TransferType.POSSIBLE;
        }
    }
}
