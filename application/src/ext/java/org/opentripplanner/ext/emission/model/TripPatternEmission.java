package org.opentripplanner.ext.emission.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Emissions for each leg in a trip-pattern(set of trip legs). The only relationship to the
 * {@link org.opentripplanner.transit.model.network.TripPattern} class is that it has the same
 * number of legs, and you should use the {@code stopPosInPattern} to get the emissions for
 * a continuous subset of legs.
 */
public class TripPatternEmission implements Serializable {

  private final List<Emission> emissionsPerLeg;

  public TripPatternEmission(Collection<Emission> emissionsPerLeg) {
    this.emissionsPerLeg = List.copyOf(emissionsPerLeg);
  }

  /**
   * Return the sum of the emissions for a subsection of the trip-pattern starting from the
   * given {@code boardStopPosInPattern} and ending at the given {@code alightStopPosInPattern}.
   */
  public Emission subsection(int boardStopPosInPattern, int alightStopPosInPattern) {
    return emissionsPerLeg
      .subList(boardStopPosInPattern, alightStopPosInPattern)
      .stream()
      .reduce(Emission.ZERO, Emission::plus);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripPatternEmission.class)
      .addCol("emissions", emissionsPerLeg)
      .toString();
  }
}
