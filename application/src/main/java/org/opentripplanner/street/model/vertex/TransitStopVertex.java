package org.opentripplanner.street.model.vertex;

import static org.opentripplanner.street.search.TraverseMode.CAR;
import static org.opentripplanner.street.search.TraverseMode.WALK;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntityLink;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StationElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitStopVertex extends StationElementVertex {

  private static final Logger LOG = LoggerFactory.getLogger(TransitStopVertex.class);
  // Do we actually need a set of modes for each stop?
  // It's nice to have for the index web API but can be generated on demand.
  private final Set<TransitMode> modes;
  private final Accessibility wheelchairAccessibility;

  private final RegularStop stop;

  /**
   * For stops that are deep underground, there is a time cost to entering and exiting the stop; all
   * stops are assumed to be at street level unless we have configuration to the contrary
   */
  private int streetToStopTime = 0;

  /**
   * @param stop  The transit model stop reference. See {@link org.opentripplanner.transit.service.TransitService#getStopVertexForStop()}
   *              for navigation from a Stop to this class.
   * @param modes Set of modes for all Routes using this stop. If {@code null} an empty set is
   *              used.
   */
  TransitStopVertex(RegularStop stop, Set<TransitMode> modes) {
    super(stop.getId(), stop.getLon(), stop.getLat(), stop.getName());
    this.stop = stop;
    this.modes = modes != null ? modes : new HashSet<>();
    this.wheelchairAccessibility = stop.getWheelchairAccessibility();
  }

  public static TransitStopVertexBuilder of() {
    return new TransitStopVertexBuilder();
  }

  public Accessibility getWheelchairAccessibility() {
    return wheelchairAccessibility;
  }

  public boolean hasPathways() {
    for (Edge e : this.getOutgoing()) {
      if (e instanceof PathwayEdge) {
        return true;
      }
    }
    for (Edge e : this.getIncoming()) {
      if (e instanceof PathwayEdge) {
        return true;
      }
    }
    return false;
  }

  public int getStreetToStopTime() {
    return streetToStopTime;
  }

  public void setStreetToStopTime(int streetToStopTime) {
    this.streetToStopTime = streetToStopTime;
    LOG.debug("Stop {} access time from street level set to {}", this, streetToStopTime);
  }

  public Set<TransitMode> getModes() {
    return modes;
  }

  public void addMode(TransitMode mode) {
    modes.add(mode);
  }

  public RegularStop getStop() {
    return this.stop;
  }

  @Override
  public StationElement getStationElement() {
    return this.stop;
  }

  /**
   * Is this vertex already linked to the street network?
   */
  public boolean isConnectedToGraph() {
    return getDegreeOut() + getDegreeIn() > 0;
  }

  /**
   * Determines if this vertex is linked (via a {@link StreetTransitEntityLink}) to a drivable edge
   * in the street network.
   * <p>
   * This method is slow: only use this during graph build.
   */
  public boolean isLinkedToDrivableEdge() {
    return isLinkedToEdgeWhichAllows(CAR);
  }

  /**
   * Determines if this vertex is linked (via a {@link StreetTransitEntityLink}) to a walkable edge
   * in the street network.
   * <p>
   * This method is slow: only use this during graph build.
   */
  public boolean isLinkedToWalkableEdge() {
    return isLinkedToEdgeWhichAllows(WALK);
  }

  private boolean isLinkedToEdgeWhichAllows(TraverseMode traverseMode) {
    return getOutgoing()
      .stream()
      .anyMatch(
        edge ->
          edge instanceof StreetTransitEntityLink<?> link &&
          link
            .getToVertex()
            .getOutgoingStreetEdges()
            .stream()
            .anyMatch(se -> se.canTraverse(traverseMode))
      );
  }
}
