package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareAttributeBuilder;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.MapUtils;

/** Responsible for mapping GTFS FareAttribute into the OTP model. */
class FareAttributeMapper {

  private final Map<org.onebusaway.gtfs.model.FareAttribute, FareAttribute> mappedStops =
    new HashMap<>();

  Collection<FareAttribute> map(Collection<org.onebusaway.gtfs.model.FareAttribute> allStops) {
    return MapUtils.mapToList(allStops, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  FareAttribute map(org.onebusaway.gtfs.model.FareAttribute orginal) {
    return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
  }

  private FareAttribute doMap(org.onebusaway.gtfs.model.FareAttribute rhs) {
    var price = Money.ofFractionalAmount(
      Currency.getInstance(rhs.getCurrencyType()),
      rhs.getPrice()
    );
    FareAttributeBuilder builder = FareAttribute.of(mapAgencyAndId(rhs.getId()))
      .setPrice(price)
      .setPaymentMethod(rhs.getPaymentMethod());

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
