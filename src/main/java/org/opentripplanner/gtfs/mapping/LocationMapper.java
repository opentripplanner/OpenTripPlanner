package org.opentripplanner.gtfs.mapping;

import java.util.Optional;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.UnsupportedGeometryException;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/** Responsible for mapping GTFS Location into the OTP model. */
public class LocationMapper {
  private static Logger LOG = LoggerFactory.getLogger(LocationMapper.class);

  private final Map<org.onebusaway.gtfs.model.Location, FlexStopLocation> mappedLocations = new HashMap<>();

  Collection<FlexStopLocation> map(Collection<org.onebusaway.gtfs.model.Location> allLocations) {
    return MapUtils.mapToList(allLocations, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe.  */
  FlexStopLocation map(org.onebusaway.gtfs.model.Location orginal) {
    return orginal == null ? null : mappedLocations.computeIfAbsent(orginal, this::doMap);
  }

  private FlexStopLocation doMap(org.onebusaway.gtfs.model.Location gtfsLocation) {
    FlexStopLocation otpLocation = new FlexStopLocation(mapAgencyAndId(gtfsLocation.getId()));

    // according to the spec stop location names are optional for flex zones so, we return the id
    // when it's null. *shrug*
    otpLocation.setName(NonLocalizedString.ofNullableOrElse(
            gtfsLocation.getName(),
            otpLocation.getId().toString())
    );
    otpLocation.setUrl(NonLocalizedString.ofNullable(gtfsLocation.getUrl()));
    otpLocation.setDescription(gtfsLocation.getDescription());
    otpLocation.setZoneId(gtfsLocation.getZoneId());
    try {
      otpLocation.setGeometry(GeometryUtils.convertGeoJsonToJtsGeometry(gtfsLocation.getGeometry()));
    }
    catch (UnsupportedGeometryException e) {
      LOG.warn("Unsupported geometry type for " + gtfsLocation.getId());
    }

    return otpLocation;
  }
}
