package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Objects;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * A configured transfer discovery profile: the user preferences the transfer paths are generated
 * and stored under, and the street search created once for it, from the routing request of the
 * profile with the preferences applied.
 * <p>
 * Duration limits and any other search-shaping detail are entirely the
 * {@link TransferPathProvider} implementation's responsibility - the regular-transfer module never
 * sees stop-classification data.
 * <p>
 * A profile is identified by its user preferences, the request and the search are derived from
 * them.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
final class TransferProfileState<P, R> {

  private final String id;
  private final AbstractUserPreferences<?> userPreferences;
  private final CostLinearFunction reuseLimit;
  private final MaxDurationsPreference maxDurations;
  private final TransferSearch<P> search;

  /**
   * @param parameters   the configuration of the profile
   * @param maxDurations the max durations of the type of the profile
   * @param pathProvider creates the search of this profile
   * @param mapper       applies the user preferences of the profile on its routing request
   */
  TransferProfileState(
    TransferProfile<R> parameters,
    MaxDurationsPreference maxDurations,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper
  ) {
    this.id = parameters.id();
    this.userPreferences = ProfileUserPreferences.resolve(parameters, mapper);
    this.reuseLimit =
      parameters.reuseLimit() == null
        ? TransferParameters.DEFAULT_REUSE_LIMIT
        : parameters.reuseLimit();
    this.maxDurations = Objects.requireNonNull(maxDurations);
    this.search = pathProvider.searchFor(
      mapper.toRequest(userPreferences, parameters.routingRequest())
    );
  }

  /** The id of the profile, for the logs and the issues reported. */
  String id() {
    return id;
  }

  /** The traveler preferences the paths of this profile are found and stored under. */
  AbstractUserPreferences<?> userPreferences() {
    return userPreferences;
  }

  /**
   * When the path of an earlier profile is used instead of a path found by this profile: the
   * tolerance, in generalized cost, of a path of the earlier profile with the cost {@code x}.
   */
  CostLinearFunction reuseLimit() {
    return reuseLimit;
  }

  /** How far the discovery search of this profile looks, the limit of the type of the profile. */
  MaxDurationsPreference maxDurations() {
    return maxDurations;
  }

  /** The street search of this profile, for its discovery search and re-costing its paths. */
  TransferSearch<P> search() {
    return search;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (TransferProfileState<?, ?>) obj;
    return userPreferences.equals(that.userPreferences);
  }

  @Override
  public int hashCode() {
    return userPreferences.hashCode();
  }

  @Override
  public String toString() {
    return "TransferProfileState[id=" + id + ", userPreferences=" + userPreferences + ']';
  }
}
