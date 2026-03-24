package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.FlexAccessEgressAdapter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.TransitServiceResolver;
import org.opentripplanner.transit.model.site.RegularStop;

public class AccessEgressMapper {

  private final TransitServiceResolver resolver;

  public AccessEgressMapper(TransitServiceResolver resolver) {
    this.resolver = resolver;
  }

  public List<RoutingAccessEgress> mapNearbyStops(
    Collection<NearbyStop> accessStops,
    AccessEgressType accessOrEgress
  ) {
    return accessStops
      .stream()
      .map(nearbyStop -> mapNearbyStop(nearbyStop, accessOrEgress))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public static Collection<RoutingAccessEgress> mapFlexAccessEgresses(
    Collection<FlexAccessEgress> flexAccessEgresses,
    AccessEgressType accessOrEgress
  ) {
    return flexAccessEgresses
      .stream()
      .map(flexAccessEgress -> new FlexAccessEgressAdapter(flexAccessEgress, accessOrEgress))
      .collect(Collectors.toList());
  }

  private RoutingAccessEgress mapNearbyStop(
    NearbyStop nearbyStop,
    AccessEgressType accessOrEgress
  ) {
    var stop = resolver.getStopLocation(nearbyStop.stopId);
    if (!(stop instanceof RegularStop)) {
      return null;
    }

    return new DefaultAccessEgress(
      stop.getIndex(),
      accessOrEgress.isEgress() ? nearbyStop.state.reverse() : nearbyStop.state
    );
  }
}
