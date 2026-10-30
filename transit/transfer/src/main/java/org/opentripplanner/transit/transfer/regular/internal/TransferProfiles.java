package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.opentripplanner.utils.collection.ParentOrder;

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

  /**
   * Create the profiles of the configuration, a search is created for each. A parent is always
   * before its children, and the order is the declared order otherwise.
   */
  static <P, R> TransferProfiles<P, R> of(
    TransferParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper
  ) {
    // Reject profiles with equal user preferences
    ProfileUserPreferences.resolveAll(parameters, mapper);
    var byId = new HashMap<String, TransferProfileState<P, R>>();
    var result = new ArrayList<TransferProfileState<P, R>>();
    for (var p : ParentOrder.parentsFirst(
      parameters.profiles(),
      TransferProfile::id,
      TransferProfile::parent
    )) {
      var profile = new TransferProfileState<>(
        p,
        parameters.maxDurations(p.type()),
        p.parent() == null ? null : byId.get(p.parent()),
        pathProvider,
        mapper
      );
      byId.put(p.id(), profile);
      result.add(profile);
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
