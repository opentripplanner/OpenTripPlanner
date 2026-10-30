package org.opentripplanner.transit.transfer.regular.api;

import java.util.Objects;

/**
 * The configuration of one walk transfer profile: a discovery search for the transfer paths of
 * walking, with its own preferences.
 *
 * @param userPreferences the traveler preferences the paths of the profile are found and stored
 *                        under. They override the equivalent values of the {@code routingRequest}.
 * @param routingRequest  the request the street search of the profile is based on
 * @param <R>             the request type, opaque to the module
 */
public record WalkProfileParameters<R>(WalkPreferences userPreferences, R routingRequest) {
  public WalkProfileParameters {
    Objects.requireNonNull(userPreferences);
    Objects.requireNonNull(routingRequest);
  }
}
