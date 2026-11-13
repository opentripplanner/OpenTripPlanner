package org.opentripplanner.transit.transfer.regular;

import java.io.Serializable;
import java.util.Set;

/**
 * A path template together with the ids of what it depends on, see
 * {@link org.opentripplanner.transit.transfer.regular.spi.TransferPath#closableFacilityIds()}. The
 * path can only be used while none of the ids is unavailable.
 *
 * @param <P>                 the street path type
 * @param path                the path template
 * @param closableFacilityIds the opaque ids the path depends on, empty if it depends on nothing.
 *                            The set is copied.
 */
public record AlternativePath<P>(P path, Set<String> closableFacilityIds) implements Serializable {
  public AlternativePath {
    closableFacilityIds = Set.copyOf(closableFacilityIds);
  }
}
