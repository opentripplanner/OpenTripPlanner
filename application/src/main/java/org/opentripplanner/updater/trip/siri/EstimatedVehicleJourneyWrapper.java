package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.trip.siri.support.NaturalLanguageStringHelper.getFirstStringFromList;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.updater.alert.siri.mapping.SiriTransportModeMapper;
import org.opentripplanner.updater.spi.UpdateErrorType;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.trip.siri.mapping.OccupancyMapper;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import uk.org.siri.siri21.DataFrameRefStructure;
import uk.org.siri.siri21.DatedVehicleJourneyRef;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.LineRef;
import uk.org.siri.siri21.OperatorRefStructure;
import uk.org.siri.siri21.VehicleJourneyRef;
import uk.org.siri.siri21.VehicleModesEnumeration;
import uk.org.siri.siri21.VehicleRef;

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
   * viewed as either a {@code ServiceJourney} or a {@code DatedServiceJourney} id. {@code empty} when
   * the journey carries no code.
   */
  Optional<EstimatedVehicleJourneyCode> code() {
    return Optional.ofNullable(code);
  }

  /**
   * The dated vehicle journey identified by unique id.
   */
  Optional<String> datedVehicleJourneyRef() {
    return Optional.ofNullable(journey.getDatedVehicleJourneyRef()).map(
      DatedVehicleJourneyRef::getValue
    );
  }

  /**
   * The dated vehicle journey identified by the pair (service journey id, service date).
   */
  Optional<VehicleJourneyIdAndServiceDate> vehicleJourneyIdAndServiceDate() {
    return Optional.ofNullable(
      VehicleJourneyIdAndServiceDate.of(journey.getFramedVehicleJourneyRef())
    );
  }

  /**
   * The reference to the vehicle operating this journey.
   * Also used for fuzzy matching
   */
  Optional<String> vehicleRef() {
    return Optional.ofNullable(journey.getVehicleRef())
      .map(VehicleRef::getValue)
      .filter(v -> !v.isBlank());
  }

  /* Replaced trips */

  /**
   * The dated vehicle journey this journey replaces.
   */
  Optional<String> replacedDatedVehicleJourneyRef() {
    return Optional.ofNullable(journey.getVehicleJourneyRef()).map(VehicleJourneyRef::getValue);
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
  Optional<String> externalLineRef() {
    return Optional.ofNullable(journey.getExternalLineRef()).map(LineRef::getValue);
  }

  /* Line, operator and mode */
  Optional<String> lineRef() {
    return Optional.ofNullable(journey.getLineRef()).map(LineRef::getValue);
  }

  Optional<String> operatorRef() {
    return Optional.ofNullable(journey.getOperatorRef()).map(OperatorRefStructure::getValue);
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

  Optional<OccupancyStatus> occupancy() {
    return Optional.ofNullable(journey.getOccupancy()).map(OccupancyMapper::mapOccupancyStatus);
  }

  Optional<String> dataSource() {
    return Optional.ofNullable(journey.getDataSource());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(journey.getClass())
      .addStr("EstimatedVehicleJourneyCode", journey.getEstimatedVehicleJourneyCode())
      .addObjOp(
        "DatedVehicleJourney",
        journey.getDatedVehicleJourneyRef(),
        DatedVehicleJourneyRef::getValue
      )
      .addObjOp("FramedVehicleJourney", journey.getFramedVehicleJourneyRef(), it ->
        ToStringBuilder.of(it.getClass())
          .addStr("VehicleJourney", it.getDatedVehicleJourneyRef())
          .addObjOp("Date", it.getDataFrameRef(), DataFrameRefStructure::getValue)
          .toString()
      )
      .addObjOp("Operator", journey.getOperatorRef(), OperatorRefStructure::getValue)
      .addCol("VehicleModes", journey.getVehicleModes())
      .addObjOp("Line", journey.getLineRef(), LineRef::getValue)
      .addObjOp("Vehicle", journey.getVehicleRef(), VehicleRef::getValue)
      .toString();
  }
}
