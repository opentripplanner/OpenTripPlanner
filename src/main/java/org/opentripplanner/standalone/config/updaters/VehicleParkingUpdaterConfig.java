package org.opentripplanner.standalone.config.updaters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class VehicleParkingUpdaterConfig {

  private static final Map<String, DataSourceType> CONFIG_MAPPING = new HashMap<>();

  static {
    CONFIG_MAPPING.put("kml", DataSourceType.KML);
    CONFIG_MAPPING.put("park-api", DataSourceType.PARK_API);
    CONFIG_MAPPING.put("bicycle-park-api", DataSourceType.BICYCLE_PARK_API);
  }

  private static DataSourceType mapStringToSourceType(String typeKey) {
    DataSourceType type = CONFIG_MAPPING.get(typeKey);
    if (type == null) {
      throw new OtpAppException("The updater source type is unknown: " + typeKey);
    }
    return type;
  }

  public static VehicleParkingUpdaterParameters create(String updaterRef, NodeAdapter c) {
    return new VehicleParkingUpdaterParameters(
        updaterRef,
        c.asText("url", null),
        c.asText("feedId", null),
        c.asText("namePrefix", null),
        c.asInt("frequencySec", 60),
        c.asBoolean("zip", false),
        c.asMap("headers", NodeAdapter::asText),
        new ArrayList<>(c.asTextSet("tags", null)),
        mapStringToSourceType(c.asText("sourceType"))
    );
  }
}
