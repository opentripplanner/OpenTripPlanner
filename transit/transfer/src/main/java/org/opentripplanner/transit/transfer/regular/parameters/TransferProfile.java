package org.opentripplanner.transit.transfer.regular.parameters;

import java.util.Objects;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * A configured transfer discovery profile: the user preferences the transfer paths are generated
 * and stored under, and the street search created once for them.
 * <p>
 * Duration limits (including any per-target-stop {@code allowedModes} override) and any other
 * search-shaping detail are entirely the {@link TransferPathProvider} implementation's
 * responsibility - the regular-transfer module never sees stop-classification data.
 * <p>
 * A profile is identified by its user preferences, the search is derived from them.
 *
 * @param <P> the street path type
 */
public final class TransferProfile<P> {

  private final AbstractUserPreferences<?> userPreferences;
  private final TransferSearch<P> search;

  /**
   * @param userPreferences the traveler preferences the paths of this profile are found and
   *                        stored under
   * @param pathProvider    creates the search of this profile for {@code userPreferences}
   */
  public TransferProfile(
    AbstractUserPreferences<?> userPreferences,
    TransferPathProvider<P> pathProvider
  ) {
    this.userPreferences = Objects.requireNonNull(userPreferences);
    this.search = pathProvider.searchFor(userPreferences);
  }

  /** The traveler preferences the paths of this profile are found and stored under. */
  public AbstractUserPreferences<?> userPreferences() {
    return userPreferences;
  }

  /** The street search of this profile, for its discovery search and re-costing its paths. */
  public TransferSearch<P> search() {
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
    var that = (TransferProfile<?>) obj;
    return userPreferences.equals(that.userPreferences);
  }

  @Override
  public int hashCode() {
    return userPreferences.hashCode();
  }

  @Override
  public String toString() {
    return "TransferProfile[userPreferences=" + userPreferences + ']';
  }
}
