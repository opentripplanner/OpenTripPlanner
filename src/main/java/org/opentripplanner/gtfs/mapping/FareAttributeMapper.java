package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/** Responsible for mapping GTFS FareAttribute into the OTP model. */
class FareAttributeMapper {
    private Map<org.onebusaway.gtfs.model.FareAttribute, FareAttribute> mappedStops = new HashMap<>();

    Collection<FareAttribute> map(Collection<org.onebusaway.gtfs.model.FareAttribute> allStops) {
        return MapUtils.mapToList(allStops, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    FareAttribute map(org.onebusaway.gtfs.model.FareAttribute orginal) {
        return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
    }

    private FareAttribute doMap(org.onebusaway.gtfs.model.FareAttribute rhs) {
        FareAttribute lhs = new FareAttribute();

        lhs.setId(mapAgencyAndId(rhs.getId()));
        lhs.setPrice(rhs.getPrice());
        lhs.setCurrencyType(rhs.getCurrencyType());
        lhs.setPaymentMethod(rhs.getPaymentMethod());
        lhs.setTransfers(rhs.getTransfers());
        lhs.setTransferDuration(rhs.getTransferDuration());
        lhs.setYouthPrice(rhs.getYouthPrice());
        lhs.setSeniorPrice(rhs.getSeniorPrice());
        lhs.setJourneyDuration(rhs.getJourneyDuration());

        return lhs;
    }
}
