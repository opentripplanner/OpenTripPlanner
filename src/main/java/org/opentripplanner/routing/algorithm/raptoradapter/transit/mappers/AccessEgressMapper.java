package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.FlexAccessEgressAdapter;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.site.RegularStop;

public class AccessEgressMapper {

  public static List<DefaultAccessEgress> mapNearbyStops(
    Collection<NearbyStop> accessStops,
    boolean isEgress
  ) {
    return accessStops
      .stream()
      .map(stopAtDistance -> mapNearbyStop(stopAtDistance, isEgress))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public static Collection<DefaultAccessEgress> mapFlexAccessEgresses(
    Collection<FlexAccessEgress> flexAccessEgresses,
    boolean isEgress
  ) {
    return flexAccessEgresses
      .stream()
      .map(flexAccessEgress -> new FlexAccessEgressAdapter(flexAccessEgress, isEgress))
      .collect(Collectors.toList());
  }

  private static DefaultAccessEgress mapNearbyStop(NearbyStop nearbyStop, boolean isEgress) {
    if (!(nearbyStop.stop instanceof RegularStop)) {
      return null;
    }

    return new DefaultAccessEgress(
      nearbyStop.stop.getIndex(),
      isEgress ? nearbyStop.state.reverse() : nearbyStop.state
    );
  }
}
