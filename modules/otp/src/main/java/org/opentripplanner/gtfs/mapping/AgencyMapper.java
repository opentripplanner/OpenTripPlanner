package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;

/** Responsible for mapping GTFS Agency into the OTP model. */
class AgencyMapper {

  private final Map<org.onebusaway.gtfs.model.Agency, Agency> mappedAgencies = new HashMap<>();

  private final String feedId;

  public AgencyMapper(String feedId) {
    this.feedId = feedId;
  }

  Collection<Agency> map(Collection<org.onebusaway.gtfs.model.Agency> agencies) {
    return MapUtils.mapToList(agencies, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Agency map(org.onebusaway.gtfs.model.Agency orginal) {
    return orginal == null ? null : mappedAgencies.computeIfAbsent(orginal, this::doMap);
  }

  private Agency doMap(org.onebusaway.gtfs.model.Agency rhs) {
    return Agency
      .of(new FeedScopedId(feedId, rhs.getId()))
      .withName(rhs.getName())
      .withTimezone(rhs.getTimezone())
      .withUrl(rhs.getUrl())
      .withLang(rhs.getLang())
      .withPhone(rhs.getPhone())
      .withFareUrl(rhs.getFareUrl())
      .withBrandingUrl(rhs.getBrandingUrl())
      .build();
  }
}
