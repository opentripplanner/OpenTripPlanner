package org.opentripplanner.routing.api.request.refactor.request;

import java.util.Set;

public class VehicleParkingRequest {
  Set<String> requiredTags = Set.of();
  Set<String> bannedTags = Set.of();
  boolean useAvailabilityInformation = false;
}
