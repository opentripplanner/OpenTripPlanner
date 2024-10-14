package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.model.Frequency;

/** Responsible for mapping GTFS Frequency into the OTP model. */
class FrequencyMapper {

  private final TripMapper tripMapper;

  private final Map<org.onebusaway.gtfs.model.Frequency, Frequency> mappedFrequencys = new HashMap<>();

  FrequencyMapper(TripMapper tripMapper) {
    this.tripMapper = tripMapper;
  }

  Collection<Frequency> map(Collection<org.onebusaway.gtfs.model.Frequency> allFrequencys) {
    return MapUtils.mapToList(allFrequencys, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Frequency map(org.onebusaway.gtfs.model.Frequency orginal) {
    return orginal == null ? null : mappedFrequencys.computeIfAbsent(orginal, this::doMap);
  }

  private Frequency doMap(org.onebusaway.gtfs.model.Frequency rhs) {
    Frequency lhs = new Frequency();

    lhs.setTrip(tripMapper.map(rhs.getTrip()));
    lhs.setStartTime(rhs.getStartTime());
    lhs.setEndTime(rhs.getEndTime());
    lhs.setHeadwaySecs(rhs.getHeadwaySecs());
    lhs.setExactTimes(rhs.getExactTimes());
    lhs.setLabelOnly(rhs.getLabelOnly());

    return lhs;
  }
}
