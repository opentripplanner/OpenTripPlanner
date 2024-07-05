package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.onebusaway.gtfs.model.Location;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.UnsupportedGeometryException;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.StopModelBuilder;

/** Responsible for mapping GTFS Location into the OTP model. */
public class LocationMapper {

  private final Map<Location, AreaStop> mappedLocations = new HashMap<>();
  private final StopModelBuilder stopModelBuilder;
  private final DataImportIssueStore issueStore;

  public LocationMapper(StopModelBuilder stopModelBuilder, DataImportIssueStore issueStore) {
    this.stopModelBuilder = stopModelBuilder;
    this.issueStore = issueStore;
  }

  Collection<AreaStop> map(Collection<Location> allLocations) {
    return MapUtils.mapToList(allLocations, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  AreaStop map(Location orginal) {
    return orginal == null ? null : mappedLocations.computeIfAbsent(orginal, this::doMap);
  }

  private AreaStop doMap(Location gtfsLocation) {
    var name = NonLocalizedString.ofNullable(gtfsLocation.getName());
    try {
      var id = mapAgencyAndId(gtfsLocation.getId());
      Geometry geometry = GeometryUtils.convertGeoJsonToJtsGeometry(gtfsLocation.getGeometry());
      var isValidOp = new IsValidOp(geometry);
      if (!isValidOp.isValid()) {
        var error = isValidOp.getValidationError();
        issueStore.add(
          Issue.issue(
            "InvalidFlexAreaGeometry",
            "GTFS flex location %s has an invalid geometry: %s at (lat: %s, lon: %s)",
            id,
            error.getMessage(),
            error.getCoordinate().y,
            error.getCoordinate().x
          )
        );
      }
      return stopModelBuilder
        .areaStop(id)
        .withName(name)
        .withUrl(NonLocalizedString.ofNullable(gtfsLocation.getUrl()))
        .withDescription(NonLocalizedString.ofNullable(gtfsLocation.getDescription()))
        .withZoneId(gtfsLocation.getZoneId())
        .withGeometry(geometry)
        .build();
    } catch (UnsupportedGeometryException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
