package org.opentripplanner.standalone.config.routerconfig.updaters;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;
import org.opentripplanner.updater.spi.HttpHeaders;

public class HttpHeadersConfig {

  public static final String PARAM_NAME = "headers";

  public static HttpHeaders headers(NodeAdapter c, OtpVersion version) {
    return HttpHeaders.of(
      c
        .of(PARAM_NAME)
        .since(version)
        .summary("HTTP headers to add to the request. Any header key, value can be inserted.")
        .asStringMap()
    );
  }
}
