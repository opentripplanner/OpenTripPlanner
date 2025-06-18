package org.opentripplanner.apis.transmodel.mapping;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.StreetMode;

class RequestStreetModesMapper {

  private static final Predicate<StreetMode> IS_BIKE_OR_CAR = m ->
    m == StreetMode.BIKE || m == StreetMode.CAR;
  private static final String ACCESS_MODE_KEY = "accessMode";
  private static final String EGRESS_MODE_KEY = "egressMode";
  private static final String DIRECT_MODE_KEY = "directMode";

  /**
   * Maps GraphQL Modes input type to RequestModes.
   * <p>
   * This only maps access, egress, direct & transfer modes. Transport modes are set using filters.
   */
  static RequestModes mapRequestStreetModes(Map<String, ?> modesInput) {
    RequestModesBuilder mBuilder = RequestModes.of();

    final StreetMode accessMode = (StreetMode) modesInput.get(ACCESS_MODE_KEY);
    ensureValueAndSet(accessMode, mBuilder::withAccessMode);
    ensureValueAndSet((StreetMode) modesInput.get(EGRESS_MODE_KEY), mBuilder::withEgressMode);
    ensureValueAndSet((StreetMode) modesInput.get(DIRECT_MODE_KEY), mBuilder::withDirectMode);
    // The only cases in which the transferMode isn't WALK are when the accessMode is either BIKE or CAR.
    // In these cases, the transferMode is the same as the accessMode. This check is not strictly necessary
    // if there is a need for more freedom for specifying the transferMode.
    Optional.ofNullable(accessMode).filter(IS_BIKE_OR_CAR).ifPresent(mBuilder::withTransferMode);

    return mBuilder.build();
  }

  /**
   * Use the provided consumer to apply the StreetMode if it's non-null, otherwise apply NOT_SET.
   *
   * @param streetMode
   * @param consumer
   */
  private static void ensureValueAndSet(StreetMode streetMode, Consumer<StreetMode> consumer) {
    consumer.accept(streetMode == null ? StreetMode.NOT_SET : streetMode);
  }
}
