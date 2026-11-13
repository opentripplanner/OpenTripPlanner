package org.opentripplanner.ext.emission.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Emissions for each hop in a trip-pattern(set of trip hops). The only relationship to the
 * {@link org.opentripplanner.transit.model.network.TripPattern} class is that it has the same
 * number of hops, and you should use the {@code stopPosInPattern} to get the emissions for a
 * continuous subset of hops.
 */
public class TripPatternEmission implements Serializable {

  private final List<Emission> emissionsPerHop;

  public TripPatternEmission(Collection<Emission> emissionsPerHop) {
    this.emissionsPerHop = List.copyOf(emissionsPerHop);
  }

  /**
   * Return the sum of the emissions for a subsection of the trip-pattern starting from the given
   * {@code boardStopPosInPattern} and ending at the given {@code alightStopPosInPattern}. Returns
   * {@code empty} if the section is outside the hops, which can happen if stops are added to the
   * trip in real time.
   */
  public Optional<Emission> section(int boardStopPosInPattern, int alightStopPosInPattern) {
    if (alightStopPosInPattern > emissionsPerHop.size()) {
      return Optional.empty();
    }
    return Optional.of(
      emissionsPerHop
        .subList(boardStopPosInPattern, alightStopPosInPattern)
        .stream()
        .reduce(Emission.ZERO, Emission::plus)
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripPatternEmission.class)
      .addCol("emissions", emissionsPerHop)
      .toString();
  }
}
