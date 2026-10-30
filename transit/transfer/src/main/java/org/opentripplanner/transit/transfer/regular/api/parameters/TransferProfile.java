package org.opentripplanner.transit.transfer.regular.api.parameters;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * The configuration of one transfer profile: a discovery search for the transfer paths of a
 * {@link TransferProfileType}, with its own preferences.
 *
 * @param id              identifies the profile in the logs and in the issues reported. It must be
 *                        unique among all the profiles.
 * @param type            the type of the profile. The user preferences of the profile are the
 *                        preferences class of the type.
 * @param parent          the id of the profile this profile is based on, {@code null} if there is
 *                        none. The parent can be of another type. The paths of the profile that are
 *                        equivalent to the paths of the parent are not stored again, see
 *                        {@link TransferParameters#DEFAULT_DEDUPLICATION_LIMIT}. The parent is generated first.
 * @param userPreferences the traveler preferences the paths of the profile are found and stored
 *                        under. They override the equivalent values of the {@code routingRequest}.
 *                        {@code null} if the profile has no preferences of its own: the user
 *                        preferences of the {@code routingRequest} are used then, see
 *                        {@link org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper#toUserPreferences}.
 * @param routingRequest  the request the street search of the profile is based on
 * @param reuseLimit      when the path of the parent is used instead of a path found by this
 *                        profile, see {@link TransferParameters#DEFAULT_REUSE_LIMIT}. {@code null}
 *                        if the profile has none of its own: it is the limit of the parent of the
 *                        same type, and the default if there is none. A limit is not inherited from
 *                        a parent of another type.
 * @param <R>             the request type, opaque to the module
 */
public record TransferProfile<R>(
  String id,
  TransferProfileType type,
  @Nullable String parent,
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
    if (parent != null && parent.isBlank()) {
      throw new IllegalArgumentException("The parent of the profile '" + id + "' is blank.");
    }
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
    @Nullable String parent,
    @Nullable AbstractUserPreferences<?> userPreferences,
    R routingRequest
  ) {
    this(id, type, parent, userPreferences, routingRequest, null);
  }

  /** A profile without a parent. */
  public TransferProfile(
    String id,
    TransferProfileType type,
    @Nullable AbstractUserPreferences<?> userPreferences,
    R routingRequest
  ) {
    this(id, type, null, userPreferences, routingRequest);
  }
}
