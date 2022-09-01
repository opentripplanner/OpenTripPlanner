package org.opentripplanner.common.model;

import java.io.Serializable;

public record RouteOriginDestination(String route, String origin, String destination)
  implements Serializable {}
