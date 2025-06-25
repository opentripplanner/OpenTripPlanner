package org.opentripplanner.apis.gtfs;

import java.util.Collection;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;

/**
 * GTFS API parameters. These parameters configure the behaviour of some aspects of the
 * GTFS GraphQL API.
 */
public interface GtfsAPIParameters {
  /**
   * Which HTTP headers or query parameters should be used as tags for performance metering in the
   * Actuator API.
   *
   * @see MicrometerGraphQLInstrumentation
   */
  Collection<String> tracingTags();
}
