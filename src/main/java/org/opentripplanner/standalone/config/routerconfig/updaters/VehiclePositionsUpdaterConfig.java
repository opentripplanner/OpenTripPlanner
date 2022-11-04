package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.vehicle_position.VehiclePositionsUpdaterParameters;

public class VehiclePositionsUpdaterConfig {

  public static VehiclePositionsUpdaterParameters create(String updaterRef, NodeAdapter c) {
    var feedId = c
      .of("feedId")
      .since(V2_2)
      .summary("Feed ID to which the update should be applied.")
      .asString();
    var frequencySec = c
      .of("frequencySec")
      .since(V2_2)
      .summary("How often the positions should be updated.")
      .asInt(60);
    var url = c
      .of("url")
      .since(V2_2)
      .summary("The URL of GTFS-RT protobuf HTTP resource to download the positions from.")
      .asUri();
    return new VehiclePositionsUpdaterParameters(updaterRef, feedId, url, frequencySec);
  }
}
