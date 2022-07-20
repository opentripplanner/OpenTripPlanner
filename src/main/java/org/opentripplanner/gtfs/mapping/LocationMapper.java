package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.UnsupportedGeometryException;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for mapping GTFS Location into the OTP model. */
public class LocationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(LocationMapper.class);

  private final Map<org.onebusaway.gtfs.model.Location, FlexStopLocation> mappedLocations = new HashMap<>();

  Collection<FlexStopLocation> map(Collection<org.onebusaway.gtfs.model.Location> allLocations) {
    return MapUtils.mapToList(allLocations, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  FlexStopLocation map(org.onebusaway.gtfs.model.Location orginal) {
    return orginal == null ? null : mappedLocations.computeIfAbsent(orginal, this::doMap);
  }

  private FlexStopLocation doMap(org.onebusaway.gtfs.model.Location gtfsLocation) {
    var name = NonLocalizedString.ofNullable(gtfsLocation.getName());
    FlexStopLocation otpLocation = new FlexStopLocation(mapAgencyAndId(gtfsLocation.getId()), name);

    otpLocation.setUrl(NonLocalizedString.ofNullable(gtfsLocation.getUrl()));
    otpLocation.setDescription(NonLocalizedString.ofNullable(gtfsLocation.getDescription()));
    otpLocation.setZoneId(gtfsLocation.getZoneId());
    try {
      otpLocation.setGeometry(
        GeometryUtils.convertGeoJsonToJtsGeometry(gtfsLocation.getGeometry())
      );
    } catch (UnsupportedGeometryException e) {
      LOG.warn("Unsupported geometry type for " + gtfsLocation.getId());
    }

    return otpLocation;
  }
}
