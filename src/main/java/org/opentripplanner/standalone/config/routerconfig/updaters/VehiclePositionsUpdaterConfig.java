package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_position.VehiclePositionsUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class VehiclePositionsUpdaterConfig {

  public static VehiclePositionsUpdaterParameters create(String updaterRef, NodeAdapter c) {
    var feedId = c.of("feedId").withDoc(NA, /*TODO DOC*/"TODO").asString();
    var frequencySec = c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60);

    var url = c.of("url").withDoc(NA, /*TODO DOC*/"TODO").asUri();
    return new VehiclePositionsUpdaterParameters(updaterRef, feedId, url, frequencySec);
  }
}
