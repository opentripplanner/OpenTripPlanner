package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.Trip;
import org.opentripplanner.util.MapUtils;

/** Responsible for mapping GTFS Frequency into the OTP model. */
class FrequencyMapper {

    private static final int EXACT_TIMES_SCHEDULE_BASED_TRIP = 1;


    private final TripMapper tripMapper;

    private Map<org.onebusaway.gtfs.model.Frequency, Frequency> mappedFrequencies = new HashMap<>();

    FrequencyMapper(TripMapper tripMapper) {
        this.tripMapper = tripMapper;
    }

    Collection<Frequency> map(Collection<org.onebusaway.gtfs.model.Frequency> allFrequencys) {
        return MapUtils.mapToList(allFrequencys, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Frequency map(org.onebusaway.gtfs.model.Frequency orginal) {
        return orginal == null ? null : mappedFrequencies.computeIfAbsent(orginal, this::doMap);
    }

    private Frequency doMap(org.onebusaway.gtfs.model.Frequency rhs) {
        Frequency lhs = new Frequency();
        Trip trip = tripMapper.map(rhs.getTrip());
        lhs.setTrip(trip);
        lhs.setStartTime(rhs.getStartTime());
        lhs.setEndTime(rhs.getEndTime());
        lhs.setHeadwaySecs(rhs.getHeadwaySecs());
        lhs.setExactHeadway(rhs.getExactTimes() == EXACT_TIMES_SCHEDULE_BASED_TRIP);
        lhs.setLabelOnly(rhs.getLabelOnly());
        return lhs;
    }
}
