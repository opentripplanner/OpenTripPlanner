package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingEntrance;

public record ParkAndRideEntranceRemoved(VehicleParkingEntrance vehicleParkingEntrance)
  implements DataImportIssue {
  private static final String FMT =
    "Park and ride entrance '%s' is removed because it's StreetVertex ('%s') is removed in a previous step.";

  @Override
  public String getMessage() {
    return String.format(
      FMT,
      vehicleParkingEntrance.getEntranceId().toString(),
      vehicleParkingEntrance.getVertex().getDefaultName()
    );
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory()
      .createPoint(vehicleParkingEntrance.getVertex().getCoordinate());
  }
}
