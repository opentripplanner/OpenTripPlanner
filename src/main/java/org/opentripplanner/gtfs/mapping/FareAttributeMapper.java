package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareAttributeBuilder;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/** Responsible for mapping GTFS FareAttribute into the OTP model. */
class FareAttributeMapper {

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

    if (rhs.getId().getAgencyId() != null && rhs.getAgencyId() != null) {
      builder.setAgency(new FeedScopedId(rhs.getId().getAgencyId(), rhs.getAgencyId()));
    }
    if (rhs.isTransfersSet()) {
      builder.setTransfers(rhs.getTransfers());
    }
    if (rhs.isTransferDurationSet()) {
      builder.setTransferDuration(rhs.getTransferDuration());
    }
    if (rhs.isJourneyDurationSet()) {
      builder.setJourneyDuration(rhs.getJourneyDuration());
    }

    return builder.build();
  }
}
