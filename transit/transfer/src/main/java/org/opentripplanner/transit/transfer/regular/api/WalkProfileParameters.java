package org.opentripplanner.transit.transfer.regular.api;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The configuration of one walk transfer profile: a discovery search for the transfer paths of
 * walking, with its own preferences.
 *
 * @param id              identifies the profile in the logs and in the issues reported. It must be
 *                        unique among all the profiles.
 * @param userPreferences the traveler preferences the paths of the profile are found and stored
 *                        under. They override the equivalent values of the {@code routingRequest}.
 *                        {@code null} if the profile has no preferences of its own: the user
 *                        preferences of the {@code routingRequest} are used then, see
 *                        {@link org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper#toUserPreferences}.
 * @param routingRequest  the request the street search of the profile is based on
 * @param <R>             the request type, opaque to the module
 */
public record WalkProfileParameters<R>(
  String id,
  @Nullable WalkPreferences userPreferences,
  R routingRequest
) {
  public WalkProfileParameters {
    Objects.requireNonNull(id);
    if (id.isBlank()) {
      throw new IllegalArgumentException("A transfer profile needs an id.");
    }
    Objects.requireNonNull(routingRequest);
  }
}
