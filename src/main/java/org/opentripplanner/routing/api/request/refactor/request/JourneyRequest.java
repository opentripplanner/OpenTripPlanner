package org.opentripplanner.routing.api.request.refactor.request;

public class JourneyRequest {
  TransitRequest transit;
  StreetRequest access;
  StreetRequest egress;
  StreetRequest transfer;
  StreetRequest direct;
}
