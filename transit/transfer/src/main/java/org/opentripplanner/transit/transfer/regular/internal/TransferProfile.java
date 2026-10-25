package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.parameters.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.parameters.WalkProfileParameters;
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
final class TransferProfile<P, R> {

  private final String id;

  @Nullable
  private final TransferProfile<P, R> parent;

  private final AbstractUserPreferences<?> userPreferences;
  private final MaxDurationsPreference maxDurations;
  private final TransferSearch<P> search;

  /**
   * @param parameters   the configuration of the profile
   * @param maxDurations the max durations of the type of the profile
   * @param parent       the profile this profile is based on, {@code null} if there is none
   * @param pathProvider creates the search of this profile
   * @param mapper       applies the user preferences of the profile on its routing request
   */
  TransferProfile(
    WalkProfileParameters<R> parameters,
    MaxDurationsPreference maxDurations,
    @Nullable TransferProfile<P, R> parent,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper
  ) {
    this.id = parameters.id();
    this.parent = parent;
    this.userPreferences = ProfileUserPreferences.resolve(parameters, mapper);
    this.maxDurations = Objects.requireNonNull(maxDurations);
    this.search = pathProvider.searchFor(
      mapper.toRequest(userPreferences, parameters.routingRequest())
    );
  }

  /** A profile without a parent. */
  TransferProfile(
    WalkProfileParameters<R> parameters,
    MaxDurationsPreference maxDurations,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper
  ) {
    this(parameters, maxDurations, null, pathProvider, mapper);
  }

  /**
   * The profile this profile is based on, {@code null} if there is none. The parent is generated
   * before this profile, and the paths of this profile that are equivalent to the paths of the
   * parent are the paths of the parent.
   */
  @Nullable
  TransferProfile<P, R> parent() {
    return parent;
  }

  /** The id of the profile, for the logs and the issues reported. */
  String id() {
    return id;
  }

  /** The traveler preferences the paths of this profile are found and stored under. */
  AbstractUserPreferences<?> userPreferences() {
    return userPreferences;
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
    var that = (TransferProfile<?, ?>) obj;
    return userPreferences.equals(that.userPreferences);
  }

  @Override
  public int hashCode() {
    return userPreferences.hashCode();
  }

  @Override
  public String toString() {
    return "TransferProfile[id=" + id + ", userPreferences=" + userPreferences + ']';
  }
}
