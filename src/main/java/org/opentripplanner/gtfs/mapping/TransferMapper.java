package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Transfer;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS Transfer into the OTP model. */
class TransferMapper {
    private final RouteMapper routeMapper;

    private final StopMapper stopMapper;

    private final TripMapper tripMapper;

    private Map<org.onebusaway.gtfs.model.Transfer, Transfer> mappedTransfers = new HashMap<>();

    TransferMapper(RouteMapper routeMapper, StopMapper stopMapper, TripMapper tripMapper) {
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
        this.tripMapper = tripMapper;
    }

    Collection<Transfer> map(Collection<org.onebusaway.gtfs.model.Transfer> allTransfers) {
        return MapUtils.mapToList(allTransfers, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Transfer map(org.onebusaway.gtfs.model.Transfer orginal) {
        return orginal == null ? null : mappedTransfers.computeIfAbsent(orginal, this::doMap);
    }

    private Transfer doMap(org.onebusaway.gtfs.model.Transfer rhs) {
        Transfer lhs = new Transfer();

        lhs.setFromStop(stopMapper.map(rhs.getFromStop()));
        lhs.setFromRoute(routeMapper.map(rhs.getFromRoute()));
        lhs.setFromTrip(tripMapper.map(rhs.getFromTrip()));
        lhs.setToStop(stopMapper.map(rhs.getToStop()));
        lhs.setToRoute(routeMapper.map(rhs.getToRoute()));
        lhs.setToTrip(tripMapper.map(rhs.getToTrip()));
        lhs.setTransferType(rhs.getTransferType());
        lhs.setMinTransferTime(rhs.getMinTransferTime());

        return lhs;
    }
}
