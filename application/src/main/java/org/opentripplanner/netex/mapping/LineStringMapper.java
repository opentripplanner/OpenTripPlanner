package org.opentripplanner.netex.mapping;

import gnu.trove.list.array.TDoubleArrayList;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.street.geometry.GeometryUtils;

/**
 * Maps a NeTEx {@link LineStringType} to a JTS {@link LineString}.
 */
class LineStringMapper {

  private final DataImportIssueStore issueStore;

  LineStringMapper(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  @Nullable
  LineString getLineString(LineStringType lineString, String issueId) {
    var coordinates = extractCoordinates(lineString);

    if (!isProjectionValid(coordinates, issueId)) {
      return null;
    }

    var geometry = GeometryUtils.makeLineString(coordinates);

    if (!isGeometryValid(geometry, issueId)) {
      return null;
    }

    return geometry;
  }

  private double[] extractCoordinates(LineStringType lineString) {
    if (lineString.getPosList() != null) {
      var values = lineString
        .getPosList()
        .getValue()
        .stream()
        .mapToDouble(Double::doubleValue)
        .toArray();
      return swapLatLon(values);
    }
    var list = new TDoubleArrayList();
    for (Object o : lineString.getPosOrPointProperty()) {
      if (o instanceof DirectPositionType directPosition) {
        var values = directPosition.getValue();
        if (values == null || values.size() != 2) {
          continue;
        }
        list.add(values.getLast());
        list.add(values.getFirst());
      } else {
        issueStore.add(
          "BadLineStringElementType",
          "Unhandled and unknown lineString element type: %s",
          o.getClass().getName()
        );
      }
    }
    return list.toArray();
  }

  /**
   * NeTEx posList/pos coordinates are ordered (latitude, longitude), but JTS
   * {@link Coordinate}s are always (x=longitude, y=latitude). Swap each pair in place so the
   * resulting array can be fed directly into {@link GeometryUtils#makeLineString(double...)}.
   */
  private static double[] swapLatLon(double[] coords) {
    for (int i = 0; i + 1 < coords.length; i += 2) {
      double lat = coords[i];
      coords[i] = coords[i + 1];
      coords[i + 1] = lat;
    }
    return coords;
  }

  private boolean isProjectionValid(double[] coordinates, String id) {
    if (coordinates.length < 4) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, " +
          "containing fewer than two coordinates for: %s",
        id
      );
      return false;
    } else if (coordinates.length % 2 != 0) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, " +
          "containing odd number of values for coordinates: %s",
        id
      );
      return false;
    }
    return true;
  }

  private boolean isGeometryValid(LineString geometry, String id) {
    Coordinate[] coordinates = geometry.getCoordinates();
    if (coordinates.length < 2) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, " +
          "containing fewer than two coordinates for: %s",
        id
      );
      return false;
    }
    if (geometry.getLength() == 0) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, having distance of 0 for: %s",
        id
      );
      return false;
    }
    for (Coordinate coordinate : coordinates) {
      if (Double.isNaN(coordinate.x) || Double.isNaN(coordinate.y)) {
        issueStore.add(
          "ServiceLinkGeometryError",
          "Ignore linkSequenceProjection with invalid linestring, " +
            "containing coordinate with NaN for: %s",
          id
        );
        return false;
      }
    }
    return true;
  }
}
