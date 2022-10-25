package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routingrequest.RoutingRequestMapper;

public class TransferRequestConfig {

  public static List<RouteRequest> map(NodeAdapter root, String transferRequestsName) {
    return root
      .of(transferRequestsName)
      .since(NA)
      .summary("TODO")
      .description(/*TODO DOC*/"TODO")
      .asObjects(List.of(new RouteRequest()), RoutingRequestMapper::mapRoutingRequest);
  }
}
