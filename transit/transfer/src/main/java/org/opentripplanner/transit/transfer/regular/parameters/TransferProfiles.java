package org.opentripplanner.transit.transfer.regular.parameters;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A validated, dependency-ordered collection of transfer profiles.
 * <p>
 * {@link #of} rejects a duplicate {@code profileId}, and a {@code deduplicationProfile}
 * referencing a profile that isn't part of the collection, before ordering the profiles so a
 * profile with a {@code deduplicationProfile} always comes after the profile it deduplicates
 * paths against. A circular {@code deduplicationProfile} reference is rejected too.
 * <p>
 * Simple, not efficient - a fixed-point iteration over the (well under 20) profiles this ever
 * holds, not a proper topological sort.
 *
 * @param <P> the street path type
 */
public final class TransferProfiles<P> implements Iterable<TransferProfile<P>> {

  private final List<TransferProfile<P>> profiles;

  private TransferProfiles(Collection<TransferProfile<P>> profiles) {
    this.profiles = List.copyOf(profiles);
  }

  public static <P> TransferProfiles<P> of(Collection<TransferProfile<P>> profiles) {
    return new TransferProfiles<>(profiles);
  }

  public int size() {
    return profiles.size();
  }

  @Override
  public Iterator<TransferProfile<P>> iterator() {
    return profiles.iterator();
  }
}
