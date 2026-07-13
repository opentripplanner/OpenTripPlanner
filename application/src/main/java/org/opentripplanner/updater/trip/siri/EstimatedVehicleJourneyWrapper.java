package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.trip.siri.support.NaturalLanguageStringHelper.getFirstStringFromList;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.updater.alert.siri.mapping.SiriTransportModeMapper;
import org.opentripplanner.updater.spi.UpdateErrorType;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.trip.siri.mapping.OccupancyMapper;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.VehicleModesEnumeration;

/**
 * A wrapper around a JAXB {@link EstimatedVehicleJourney} that also owns the parsed and validated
 * {@link CallWrapper}s for that journey.
 */
final class EstimatedVehicleJourneyWrapper {

  private final EstimatedVehicleJourney journey;
  private final List<CallWrapper> calls;

  @Nullable
  private final EstimatedVehicleJourneyCode code;

  private EstimatedVehicleJourneyWrapper(EstimatedVehicleJourney journey, List<CallWrapper> calls) {
    this.journey = journey;
    this.calls = calls;
    this.code = journey.getEstimatedVehicleJourneyCode() != null
      ? new EstimatedVehicleJourneyCode(journey.getEstimatedVehicleJourneyCode())
      : null;
  }

  /* Construction and validation */

  static EstimatedVehicleJourneyWrapper of(EstimatedVehicleJourney journey) throws UpdateException {
    var wrapper = new EstimatedVehicleJourneyWrapper(journey, CallWrapper.of(journey));
    wrapper.validate();
    return wrapper;
  }

  private void validate() throws UpdateException {
    if (mustBeRejectedAsUnmonitored()) {
      throw UpdateException.of(UpdateErrorType.NOT_MONITORED);
    }
  }

  /**
   * Whether this journey must be rejected because it is not monitored.
   * <p>
   * A journey reported as not monitored is normally rejected, but the not-monitored flag is ignored
   * when the journey is a cancellation: a cancelled journey is no longer monitored, yet must still
   * be processed so that the cancellation is applied.
   */
  private boolean mustBeRejectedAsUnmonitored() {
    return !isMonitored() && !isCancellation();
  }

  /* Calls */

  List<CallWrapper> calls() {
    return calls;
  }

  /**
   * Whether at least one call of this journey is an extra (unplanned) call.
   */
  boolean hasExtraCall() {
    return calls.stream().anyMatch(CallWrapper::isExtraCall);
  }

  /* Journey status */

  boolean isMonitored() {
    return TRUE.equals(journey.isMonitored());
  }

  boolean isCancellation() {
    return TRUE.equals(journey.isCancellation());
  }

  boolean isExtraJourney() {
    return TRUE.equals(journey.isExtraJourney());
  }

  boolean isPredictionInaccurate() {
    return TRUE.equals(journey.isPredictionInaccurate());
  }

  /* Trip identification */

  /**
   * The EstimatedVehicleJourneyCode of an extra journey, used to identify the added trip. It can be
   * viewed as either a {@code ServiceJourney} or a {@code DatedServiceJourney} id. {@code null} when
   * the journey carries no code.
   */
  @Nullable
  EstimatedVehicleJourneyCode code() {
    return code;
  }

  /**
   * The dated vehicle journey identified by unique id.
   */
  @Nullable
  String datedVehicleJourneyRef() {
    return journey.getDatedVehicleJourneyRef() != null
      ? journey.getDatedVehicleJourneyRef().getValue()
      : null;
  }

  /**
   * The dated vehicle journey identified by the pair (service journey id, service date).
   */
  @Nullable
  VehicleJourneyIdAndServiceDate vehicleJourneyIdAndServiceDate() {
    return VehicleJourneyIdAndServiceDate.of(journey.getFramedVehicleJourneyRef());
  }

  /**
   * An internal (private) id used for fuzzy matching.
   */
  @Nullable
  String internalPlanningCode() {
    return journey.getVehicleRef() != null ? journey.getVehicleRef().getValue() : null;
  }

  /* Replaced trips */

  /**
   * The dated vehicle journey this journey replaces.
   */
  @Nullable
  String replacedDatedVehicleJourneyRef() {
    return journey.getVehicleJourneyRef() != null
      ? journey.getVehicleJourneyRef().getValue()
      : null;
  }

  /**
   * Additional dated vehicle journeys this journey replaces (beyond {@link #replacedDatedVehicleJourneyRef()}).
   */
  List<VehicleJourneyIdAndServiceDate> additionalReplacedDatedVehicleJourneyRefs() {
    return journey
      .getAdditionalVehicleJourneyReves()
      .stream()
      .map(VehicleJourneyIdAndServiceDate::of)
      .toList();
  }

  /**
   * In case of a replacement departure, the line of the replaced vehicle journey.
   */
  @Nullable
  String externalLineRef() {
    return journey.getExternalLineRef() != null ? journey.getExternalLineRef().getValue() : null;
  }

  /* Line, operator and mode */

  @Nullable
  String lineRef() {
    return journey.getLineRef() != null ? journey.getLineRef().getValue() : null;
  }

  @Nullable
  String operatorRef() {
    return journey.getOperatorRef() != null ? journey.getOperatorRef().getValue() : null;
  }

  /**
   * Whether this journey is operated by rail.
   */
  boolean isRail() {
    return journey.getVehicleModes().contains(VehicleModesEnumeration.RAIL);
  }

  /**
   * The OTP transit mode of this journey, derived from its SIRI vehicle modes.
   */
  TransitMode transitMode() {
    return SiriTransportModeMapper.mapTransitMainMode(journey.getVehicleModes());
  }

  /* Descriptive information */

  /**
   * The published line name, or an empty string if not set.
   */
  String publishedLineName() {
    return getFirstStringFromList(journey.getPublishedLineNames());
  }

  /**
   * The destination name (headsign), or an empty string if not set.
   */
  String destinationName() {
    return getFirstStringFromList(journey.getDestinationNames());
  }

  @Nullable
  OccupancyStatus occupancy() {
    return journey.getOccupancy() == null
      ? null
      : OccupancyMapper.mapOccupancyStatus(journey.getOccupancy());
  }

  @Nullable
  String dataSource() {
    return journey.getDataSource();
  }

  /**
   * A human-readable, single-line description of this journey for debug logging.
   */
  String debugString() {
    return DebugString.of(journey);
  }
}
