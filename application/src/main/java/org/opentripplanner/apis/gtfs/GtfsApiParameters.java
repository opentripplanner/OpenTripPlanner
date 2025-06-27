package org.opentripplanner.apis.gtfs;

import java.util.Collection;

/**
 * GTFS API parameters. These parameters configure the behaviour of some aspects of the
 * GTFS GraphQL API.
 */
public interface GtfsApiParameters {
  /**
   * Which HTTP headers or query parameters should be used as tags for performance metering in the
   * Actuator API.
   *
   * @see org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation
   */
  Collection<String> tracingTags();
}
