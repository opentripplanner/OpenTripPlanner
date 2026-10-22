package org.opentripplanner.transit.transfer.regular.parameters;

import java.util.Objects;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * A configured transfer discovery profile: the routing request the street search is configured
 * with, and the user preferences the transfer paths are generated and stored under.
 * <p>
 * Duration limits (including any per-target-stop {@code allowedModes} override) and any other
 * search-shaping detail are entirely the {@link TransferPathProvider} implementation's
 * responsibility - the regular-transfer module never sees stop-classification data, and the
 * routing request type is opaque to it.
 *
 * @param <R> the routing request type
 */
public final class TransferProfile<R> {

  private final R configuredRequestParameters;
  private final AbstractUserPreferences<?> userPreferences;

  /**
   * @param configuredRequestParameters this profile's static, config-derived routing request
   *                                    (street mode, duration limits, ...) - opaque to this
   *                                    module, only the {@link TransferPathProvider}
   *                                    implementation interprets it.
   * @param userPreferences             the traveler preferences, used both for this profile's own
   *                                    discovery search and to re-cost a
   *                                    {@code deduplicationProfile}'s path when checking for
   *                                    equivalence
   */
  public TransferProfile(
    R configuredRequestParameters,
    AbstractUserPreferences<?> userPreferences
  ) {
    this.configuredRequestParameters = configuredRequestParameters;
    this.userPreferences = userPreferences;
  }

  // /**
  //  * This profile's static, config-derived routing request (street mode, duration limits, ...) -
  //  * opaque to this module, only the {@link TransferPathProvider} implementation interprets it.
  //  */
  // TODO: Use the request to shape the street search, and/or to re-cost a deduplicationProfile's
  //       path when checking for equivalence
  // public R configuredRequestParameters() {
  //    return configuredRequestParameters;
  // }

  /**
   * The traveler preferences, used both for this profile's own discovery search and to re-cost a
   * {@code deduplicationProfile}'s path when checking for equivalence
   */
  public AbstractUserPreferences<?> userPreferences() {
    return userPreferences;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (TransferProfile) obj;
    return (
      Objects.equals(this.configuredRequestParameters, that.configuredRequestParameters) &&
      Objects.equals(this.userPreferences, that.userPreferences)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(configuredRequestParameters, userPreferences);
  }

  @Override
  public String toString() {
    return (
      "RegularTransferParameters[" +
      "requestParameters=" +
      configuredRequestParameters +
      ", " +
      "userPreferences=" +
      userPreferences +
      ']'
    );
  }
}
