package org.opentripplanner.transit.transfer.regular.api.parameters;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * The configuration of one transfer profile: a discovery search for the transfer paths of a
 * {@link TransferProfileType}, with its own preferences.
 *
 * @param id              identifies the profile. It must be unique among all the profiles.
 * @param type            the type of the profile. The user preferences of the profile are the
 *                        preferences class of the type.
 * @param userPreferences the traveler preferences the paths of the profile are found and stored
 *                        under. They override the equivalent values of the {@code routingRequest}.
 *                        {@code null} if the profile has no preferences of its own: the user
 *                        preferences of the {@code routingRequest} are used then, see
 *                        {@link org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper#toUserPreferences}.
 * @param routingRequest  the request the street search of the profile is based on
 * @param reuseLimit      when the path of an earlier profile is used instead of a path found by
 *                        this profile, see {@link TransferParameters#DEFAULT_REUSE_LIMIT}.
 *                        {@code null} if the profile has none of its own: the default is used.
 * @param <R>             the request type, opaque to the module
 */
public record TransferProfile<R>(
  String id,
  TransferProfileType type,
  @Nullable AbstractUserPreferences<?> userPreferences,
  R routingRequest,
  @Nullable CostLinearFunction reuseLimit
) {
  public TransferProfile {
    Objects.requireNonNull(id);
    Objects.requireNonNull(type);
    if (id.isBlank()) {
      throw new IllegalArgumentException("A transfer profile needs an id.");
    }
    Objects.requireNonNull(routingRequest);
    if (userPreferences != null && userPreferences.type() != type) {
      throw new IllegalArgumentException(
        "The user preferences of the profile '" +
          id +
          "' are the preferences of the type " +
          userPreferences.type() +
          ", not " +
          type
      );
    }
  }

  /** A profile without a limit of its own. */
  public TransferProfile(
    String id,
    TransferProfileType type,
    @Nullable AbstractUserPreferences<?> userPreferences,
    R routingRequest
  ) {
    this(id, type, userPreferences, routingRequest, null);
  }
}
