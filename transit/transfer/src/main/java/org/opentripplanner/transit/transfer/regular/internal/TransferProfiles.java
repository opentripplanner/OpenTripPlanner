package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * The transfer profiles, in the order they are declared in the configuration.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
final class TransferProfiles<P, R> implements Iterable<TransferProfileState<P, R>> {

  private final List<TransferProfileState<P, R>> profiles;

  private TransferProfiles(Collection<TransferProfileState<P, R>> profiles) {
    this.profiles = List.copyOf(profiles);
  }

  /** The profiles in the given order. */
  static <P, R> TransferProfiles<P, R> of(Collection<TransferProfileState<P, R>> profiles) {
    return new TransferProfiles<>(profiles);
  }

  /** Create the profiles of the configuration in the declared order, with a search for each. */
  static <P, R> TransferProfiles<P, R> of(
    TransferParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper
  ) {
    // Reject profiles with equal user preferences
    ProfileUserPreferences.resolveAll(parameters, mapper);
    var result = new ArrayList<TransferProfileState<P, R>>();
    for (var p : parameters.profiles()) {
      result.add(
        new TransferProfileState<>(p, parameters.maxDurations(p.type()), pathProvider, mapper)
      );
    }
    return new TransferProfiles<>(result);
  }

  int size() {
    return profiles.size();
  }

  @Override
  public Iterator<TransferProfileState<P, R>> iterator() {
    return profiles.iterator();
  }
}
