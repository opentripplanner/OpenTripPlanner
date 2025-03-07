package org.opentripplanner.apis.gtfs.mapping;

import java.util.Map;
import org.opentripplanner.framework.geometry.WgsCoordinate;

public class CoordinateMapper {

  public static WgsCoordinate mapCoordinate(Map<String, Double> coordinate) {
    return new WgsCoordinate(coordinate.get("latitude"), coordinate.get("longitude"));
  }
}
