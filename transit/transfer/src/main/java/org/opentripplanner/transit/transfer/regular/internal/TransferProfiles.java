package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.transit.transfer.regular.api.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * The transfer profiles, in the order they are declared in the configuration.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
final class TransferProfiles<P, R> implements Iterable<TransferProfile<P, R>> {

  private final List<TransferProfile<P, R>> profiles;

  private TransferProfiles(Collection<TransferProfile<P, R>> profiles) {
    this.profiles = List.copyOf(profiles);
  }

  static <P, R> TransferProfiles<P, R> of(Collection<TransferProfile<P, R>> profiles) {
    return new TransferProfiles<>(profiles);
  }

  /** Create the profiles of the configuration, a search is created for each. */
  static <P, R> TransferProfiles<P, R> of(
    TransferProfilesParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper
  ) {
    // Reject profiles with equal user preferences
    ProfileUserPreferences.resolveAll(parameters, mapper);
    return of(
      parameters
        .walk()
        .stream()
        .map(p ->
          new TransferProfile<>(
            p,
            parameters.maxDurations(TransferProfileType.WALK),
            pathProvider,
            mapper
          )
        )
        .toList()
    );
  }

  int size() {
    return profiles.size();
  }

  @Override
  public Iterator<TransferProfile<P, R>> iterator() {
    return profiles.iterator();
  }
}
