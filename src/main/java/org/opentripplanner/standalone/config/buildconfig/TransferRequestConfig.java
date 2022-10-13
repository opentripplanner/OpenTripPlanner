package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.RoutingRequestMapper;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransferRequestConfig {

  public static List<RouteRequest> map(NodeAdapter root, String transferRequestsName) {
    return root
      .of(transferRequestsName)
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withExample(/*TODO DOC*/"TODO")
      .withDescription(/*TODO DOC*/"TODO")
      .asObjects(List.of(new RouteRequest()), RoutingRequestMapper::mapRoutingRequest);
  }
}
