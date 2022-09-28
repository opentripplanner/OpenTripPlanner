package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.kml.KmlUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.parkapi.ParkAPIUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
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

  public static VehicleParkingUpdaterParameters create(String updaterRef, NodeAdapter c) {
    var sourceType = c.asEnum("sourceType", DataSourceType.class);
    var feedId = c.asText("feedId", null);
    var timeZone = c.asZoneId("timeZone", null);
    switch (sourceType) {
      case HSL_PARK:
        return new HslParkUpdaterParameters(
          updaterRef,
          c.of("facilitiesFrequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(3600),
          c.asText("facilitiesUrl", null),
          feedId,
          sourceType,
          c.of("utilizationsFrequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(600),
          c.asText("utilizationsUrl", null),
          timeZone
        );
      case KML:
        return new KmlUpdaterParameters(
          updaterRef,
          c.asText("url", null),
          feedId,
          c.asText("namePrefix", null),
          c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
          c.of("zip").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
          sourceType
        );
      case PARK_API:
      case BICYCLE_PARK_API:
        return new ParkAPIUpdaterParameters(
          updaterRef,
          c.asText("url", null),
          feedId,
          c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
          c.asStringMap("headers"),
          new ArrayList<>(c.asTextSet("tags", Set.of())),
          sourceType,
          timeZone
        );
      default:
        throw new OtpAppException("The updater source type is unhandled: " + sourceType);
    }
  }
}
