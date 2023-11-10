package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.UnsupportedGeometryException;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.StopModelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for mapping GTFS Location into the OTP model. */
public class LocationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(LocationMapper.class);

  private final Map<org.onebusaway.gtfs.model.Location, AreaStop> mappedLocations = new HashMap<>();
  private final StopModelBuilder stopModelBuilder;

  public LocationMapper(StopModelBuilder stopModelBuilder) {
    this.stopModelBuilder = stopModelBuilder;
  }

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

    return stopModelBuilder
      .areaStop(mapAgencyAndId(gtfsLocation.getId()))
      .withName(name)
      .withUrl(NonLocalizedString.ofNullable(gtfsLocation.getUrl()))
      .withDescription(NonLocalizedString.ofNullable(gtfsLocation.getDescription()))
      .withZoneId(gtfsLocation.getZoneId())
      .withGeometry(geometry)
      .build();
  }
}
