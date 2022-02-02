package org.opentripplanner.standalone.config.updaters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.kml.KmlUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.parkapi.ParkAPIUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class VehicleParkingUpdaterConfig {

  private static final Map<String, DataSourceType> CONFIG_MAPPING = new HashMap<>();

  static {
    CONFIG_MAPPING.put("hsl-park", DataSourceType.HSL_PARK);
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
    var sourceType = mapStringToSourceType(c.asText("sourceType"));
    var feedId = c.asText("feedId", null);
    switch (sourceType) {
      case HSL_PARK:
        return new HslParkUpdaterParameters(
                updaterRef, 
                c.asInt("facilitiesFrequencySec", 3600),
                c.asText("facilitiesUrl", null),
                feedId,
                sourceType,
                c.asInt("utilizationsFrequencySec", 600),
                c.asText("utilizationsUrl", null)
        );
      case KML:
        return new KmlUpdaterParameters(
                updaterRef,
                c.asText("url", null),
                feedId,
                c.asText("namePrefix", null),
                c.asInt("frequencySec", 60),
                c.asBoolean("zip", false),
                sourceType
        );
      case PARK_API:
      case BICYCLE_PARK_API:
        return new ParkAPIUpdaterParameters(
                updaterRef,
                c.asText("url", null),
                feedId,
                c.asInt("frequencySec", 60),
                c.asMap("headers", NodeAdapter::asText),
                new ArrayList<>(c.asTextSet("tags", Set.of())),
                sourceType
        );
      default:
        throw new OtpAppException("The updater source type is unhandled: " + sourceType);
    }
  }
}
