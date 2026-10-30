package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.transit.transfer.regular.api.parameters.DeduplicationLimit;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.opentripplanner.utils.collection.ParentOrder;

/**
 * The transfer profiles, in the order they are declared in the configuration.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
final class TransferProfiles<P, R> implements Iterable<TransferProfile<P, R>> {

  private final List<TransferProfile<P, R>> profiles;
  private final DeduplicationLimit deduplicationLimit;

  private TransferProfiles(
    Collection<TransferProfile<P, R>> profiles,
    DeduplicationLimit deduplicationLimit
  ) {
    this.profiles = List.copyOf(profiles);
    this.deduplicationLimit = deduplicationLimit;
  }

  /** The profiles in the given order, with the default deduplication limit. */
  static <P, R> TransferProfiles<P, R> of(Collection<TransferProfile<P, R>> profiles) {
    return new TransferProfiles<>(profiles, DeduplicationLimit.DEFAULT);
  }

  /**
   * Create the profiles of the configuration, a search is created for each. A parent is always
   * before its children, and the order is the declared order otherwise.
   */
  static <P, R> TransferProfiles<P, R> of(
    TransferProfilesParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper
  ) {
    // Reject profiles with equal user preferences
    ProfileUserPreferences.resolveAll(parameters, mapper);
    var byId = new HashMap<String, TransferProfile<P, R>>();
    var result = new ArrayList<TransferProfile<P, R>>();
    for (var p : ParentOrder.parentsFirst(
      parameters.profiles(),
      TransferProfileParameters::id,
      TransferProfileParameters::parent
    )) {
      var profile = new TransferProfile<>(
        p,
        parameters.maxDurations(p.type()),
        p.parent() == null ? null : byId.get(p.parent()),
        pathProvider,
        mapper
      );
      byId.put(p.id(), profile);
      result.add(profile);
    }
    return new TransferProfiles<>(result, parameters.deduplicationLimit());
  }

  /** When the path of a parent is used instead of the path of a child. */
  DeduplicationLimit deduplicationLimit() {
    return deduplicationLimit;
  }

  int size() {
    return profiles.size();
  }

  @Override
  public Iterator<TransferProfile<P, R>> iterator() {
    return profiles.iterator();
  }
}
