package org.opentripplanner.ext.flex.template;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.site.StopLocation;

public class FlexDirectPathFactory {

  private final FlexAccessEgressCallbackAdapter callbackService;
  private final FlexPathCalculator accessPathCalculator;
  private final FlexPathCalculator egressPathCalculator;
  private final Duration maxTransferDuration;

  public FlexDirectPathFactory(
    FlexAccessEgressCallbackAdapter callbackService,
    FlexPathCalculator accessPathCalculator,
    FlexPathCalculator egressPathCalculator,
    Duration maxTransferDuration
  ) {
    this.callbackService = callbackService;
    this.accessPathCalculator = accessPathCalculator;
    this.egressPathCalculator = egressPathCalculator;
    this.maxTransferDuration = maxTransferDuration;
  }

  public Collection<DirectFlexPath> calculateDirectFlexPaths(
    Collection<NearbyStop> streetAccesses,
    Collection<NearbyStop> streetEgresses,
    List<FlexServiceDate> dates,
    int departureTime,
    boolean arriveBy
  ) {
    Collection<DirectFlexPath> directFlexPaths = new ArrayList<>();

    var flexAccessTemplates = new FlexAccessFactory(
      callbackService,
      accessPathCalculator,
      maxTransferDuration
    )
      .calculateFlexAccessTemplates(streetAccesses, dates);

    var flexEgressTemplates = new FlexEgressFactory(
      callbackService,
      egressPathCalculator,
      maxTransferDuration
    )
      .calculateFlexEgressTemplates(streetEgresses, dates);

    Multimap<StopLocation, NearbyStop> streetEgressByStop = HashMultimap.create();
    streetEgresses.forEach(it -> streetEgressByStop.put(it.stop, it));

    for (FlexAccessTemplate template : flexAccessTemplates) {
      StopLocation transferStop = template.getTransferStop();

      // TODO: Document or reimplement this. Why are we using the egress to see if the
      //      access-transfer-stop (last-stop) is used by at least one egress-template?
      //      Is it because:
      //      - of the group-stop expansion?
      //      - of the alight-restriction check?
      //      - nearest stop to trip match?
      //      Fix: Find out why and refactor out the business logic and reuse it.
      //      Problem: Any asymmetrical restriction witch apply/do not apply to the egress,
      //               but do not apply/apply to the access, like booking-notice.
      if (
        flexEgressTemplates.stream().anyMatch(t -> t.getAccessEgressStop().equals(transferStop))
      ) {
        for (NearbyStop egress : streetEgressByStop.get(transferStop)) {
          template
            .createDirectGraphPath(egress, arriveBy, departureTime)
            .ifPresent(directFlexPaths::add);
        }
      }
    }

    return directFlexPaths;
  }
}
