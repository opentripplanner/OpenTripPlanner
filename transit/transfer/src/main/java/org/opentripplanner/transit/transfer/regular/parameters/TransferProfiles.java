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
 */
public final class TransferProfiles<R> implements Iterable<TransferProfile<R>> {

  private final List<TransferProfile<R>> profiles;

  private TransferProfiles(Collection<TransferProfile<R>> profiles) {
    this.profiles = List.copyOf(profiles);
  }

  public static <R> TransferProfiles<R> of(Collection<TransferProfile<R>> profiles) {
    return new TransferProfiles<>(profiles);
  }

  @Override
  public Iterator<TransferProfile<R>> iterator() {
    return profiles.iterator();
  }
}
