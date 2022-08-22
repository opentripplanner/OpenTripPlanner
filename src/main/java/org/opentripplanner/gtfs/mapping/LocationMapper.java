package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.UnsupportedGeometryException;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.geometry.GeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for mapping GTFS Location into the OTP model. */
public class LocationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(LocationMapper.class);

  private final Map<org.onebusaway.gtfs.model.Location, AreaStop> mappedLocations = new HashMap<>();

  Collection<AreaStop> map(Collection<org.onebusaway.gtfs.model.Location> allLocations) {
    return MapUtils.mapToList(allLocations, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  AreaStop map(org.onebusaway.gtfs.model.Location orginal) {
    return orginal == null ? null : mappedLocations.computeIfAbsent(orginal, this::doMap);
  }

  private AreaStop doMap(org.onebusaway.gtfs.model.Location gtfsLocation) {
    var name = NonLocalizedString.ofNullable(gtfsLocation.getName());
    Geometry geometry = null;
    try {
      geometry = GeometryUtils.convertGeoJsonToJtsGeometry(gtfsLocation.getGeometry());
    } catch (UnsupportedGeometryException e) {
      LOG.error("Unsupported geometry type for {}", gtfsLocation.getId());
    }

    return AreaStop
      .of(mapAgencyAndId(gtfsLocation.getId()))
      .withName(name)
      .withUrl(NonLocalizedString.ofNullable(gtfsLocation.getUrl()))
      .withDescription(NonLocalizedString.ofNullable(gtfsLocation.getDescription()))
      .withZoneId(gtfsLocation.getZoneId())
      .withGeometry(geometry)
      .build();
  }
}
