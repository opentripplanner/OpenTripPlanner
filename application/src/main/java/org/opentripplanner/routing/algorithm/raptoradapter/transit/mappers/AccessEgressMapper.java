package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.FlexAccessEgressAdapter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.site.RegularStop;

public class AccessEgressMapper {

  public static List<RoutingAccessEgress> mapNearbyStops(
    Collection<NearbyStop> accessStops
  ) {
    return accessStops
      .stream()
      .map(AccessEgressMapper::mapNearbyStop)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public static Collection<RoutingAccessEgress> mapFlexAccessEgresses(
    Collection<FlexAccessEgress> flexAccessEgresses
  ) {
    return flexAccessEgresses
      .stream()
      .map(FlexAccessEgressAdapter::new)
      .collect(Collectors.toList());
  }

  private static RoutingAccessEgress mapNearbyStop(NearbyStop nearbyStop) {
    if (!(nearbyStop.stop instanceof RegularStop)) {
      return null;
    }

    return new DefaultAccessEgress(nearbyStop.stop.getIndex(), nearbyStop.state);
  }
}
