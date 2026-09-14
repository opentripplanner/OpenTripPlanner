package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import org.opentripplanner.graph_builder.module.transfer.api.TransferProfilesConfig;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;

/**
 * {@code RouteRequest -> RaptorTransferProfile} mapping, used at request time to pick which
 * profile's {@link org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService}
 * should serve a live request. An unmatched request falls back to the first configured profile
 * (see {@link TransferProfilesConfig#fallbackProfile()}), matching today's behavior of always
 * having some transfer answer.
 */
public final class RaptorTransferProfileMapper {

  private RaptorTransferProfileMapper() {}

  public static RaptorTransferProfile fromRouteRequest(
    RouteRequest request,
    TransferProfilesConfig config
  ) {
    if (request.journey().wheelchair()) {
      return matchOrFallback(RaptorTransferProfile.WHEELCHAIR, config);
    }
    RaptorTransferProfile profileId = switch (request.journey().transfer().mode()) {
      case WALK -> RaptorTransferProfile.WALK;
      case BIKE -> RaptorTransferProfile.BICYCLE;
      case CAR -> RaptorTransferProfile.CAR;
      case SCOOTER_RENTAL -> RaptorTransferProfile.SCOOTER;
      default -> null;
    };
    return matchOrFallback(profileId, config);
  }

  private static RaptorTransferProfile matchOrFallback(
    RaptorTransferProfile profileId,
    TransferProfilesConfig config
  ) {
    boolean configured =
      profileId != null &&
      config
        .profiles()
        .stream()
        .anyMatch(p -> p.profileId() == profileId);
    return configured ? profileId : config.fallbackProfile().profileId();
  }
}
