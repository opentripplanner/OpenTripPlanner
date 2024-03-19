package org.opentripplanner.apis.gtfs.model;

import graphql.relay.ConnectionCursor;
import java.time.Duration;

public record PlanPageInfo(
  ConnectionCursor startCursor,
  ConnectionCursor endCursor,
  boolean hasPreviousPage,
  boolean hasNextPage,
  Duration searchWindowUsed
) {}
