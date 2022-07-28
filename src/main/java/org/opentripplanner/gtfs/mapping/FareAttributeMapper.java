package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareAttributeBuilder;
import org.opentripplanner.util.MapUtils;

/** Responsible for mapping GTFS FareAttribute into the OTP model. */
class FareAttributeMapper {

  private static final int MISSING_VALUE = -999;

  private final Map<org.onebusaway.gtfs.model.FareAttribute, FareAttribute> mappedStops = new HashMap<>();

  Collection<FareAttribute> map(Collection<org.onebusaway.gtfs.model.FareAttribute> allStops) {
    return MapUtils.mapToList(allStops, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  FareAttribute map(org.onebusaway.gtfs.model.FareAttribute orginal) {
    return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
  }

  private FareAttribute doMap(org.onebusaway.gtfs.model.FareAttribute rhs) {
    FareAttributeBuilder builder = FareAttribute
      .of(mapAgencyAndId(rhs.getId()))
      .setPrice(rhs.getPrice())
      .setCurrencyType(rhs.getCurrencyType())
      .setPaymentMethod(rhs.getPaymentMethod())
      .setYouthPrice(rhs.getYouthPrice())
      .setSeniorPrice(rhs.getSeniorPrice());

    if (rhs.getTransfers() != MISSING_VALUE) {
      builder.setTransfers(rhs.getTransfers());
    }
    if (rhs.getTransferDuration() != MISSING_VALUE) {
      builder.setTransferDuration(rhs.getTransferDuration());
    }
    if (rhs.getJourneyDuration() != MISSING_VALUE) {
      builder.setJourneyDuration(rhs.getJourneyDuration());
    }

    return builder.build();
  }
}
