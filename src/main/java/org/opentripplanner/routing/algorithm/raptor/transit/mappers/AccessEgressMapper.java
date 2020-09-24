package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccessEgressMapper {

  private final StopIndexForRaptor stopIndex;

  public AccessEgressMapper(StopIndexForRaptor stopIndex) {
    this.stopIndex = stopIndex;
  }

  public AccessEgress mapNearbyStop(NearbyStop nearbyStop, boolean isEgress) {
    if (!(nearbyStop.stop instanceof Stop)) { return null; }
    return new AccessEgress(
        stopIndex.indexByStop.get(nearbyStop.stop),
        (int) nearbyStop.state.getElapsedTimeSeconds(),
        isEgress ? nearbyStop.state.reverse() : nearbyStop.state
    );
  }

  public List<AccessEgress> mapNearbyStops(Collection<NearbyStop> accessStops, boolean isEgress) {
    return accessStops
        .stream()
        .map(stopAtDistance -> mapNearbyStop(stopAtDistance, isEgress))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public Collection<AccessEgress> mapFlexAccessEgresses(
      Collection<FlexAccessEgress> flexAccessEgresses
  ) {
    return flexAccessEgresses.stream().map(FlexAccessEgressAdapter::new).collect(Collectors.toList());
  }

  private class FlexAccessEgressAdapter extends AccessEgress {
    private final FlexAccessEgress flexAccessEgress;

    public FlexAccessEgressAdapter(FlexAccessEgress flexAccessEgress) {
      super(
          stopIndex.indexByStop.get(flexAccessEgress.stop),
          flexAccessEgress.preFlexTime + flexAccessEgress.flexTime + flexAccessEgress.postFlexTime,
          flexAccessEgress.lastState
      );

      this.flexAccessEgress = flexAccessEgress;
    }

    @Override
    public int earliestDepartureTime(int requestedDepartureTime) {
      return flexAccessEgress.earliestDepartureTime(requestedDepartureTime);
    }

    @Override
    public int latestArrivalTime(int requestedArrivalTime) {
      return flexAccessEgress.latestArrivalTime(requestedArrivalTime);
    }

    @Override
    public int numberOfLegs() {
      return flexAccessEgress.directToStop ? 2 : 3;
    }

    @Override
    public boolean stopReachedOnBoard() {
      return flexAccessEgress.directToStop;
    }
  }
}
