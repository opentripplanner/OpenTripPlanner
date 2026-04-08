package org.opentripplanner.updater.spi;

public enum UpdateErrorType {
  UNKNOWN,
  INVALID_INPUT_STRUCTURE,
  TRIP_NOT_FOUND,
  TRIP_NOT_FOUND_IN_PATTERN,
  NO_FUZZY_TRIP_MATCH,
  MULTIPLE_FUZZY_TRIP_MATCHES,
  EMPTY_STOP_POINT_REF,
  MISSING_CALL_ORDER,
  MIXED_CALL_ORDER_AND_VISIT_NUMBER,
  NO_TRIP_FOR_CANCELLATION_FOUND,
  TRIP_ALREADY_EXISTS,
  NO_START_DATE,
  NO_UPDATES,
  NO_TRIP_ID,
  TOO_FEW_STOPS,
  TOO_MANY_STOPS,
  NO_VALID_STOPS,
  // the stop cannot be found in the site repository
  UNKNOWN_STOP,
  // the stop exists in the site repository, but the planned stop cannot be replaced by this stop
  // since they do not belong to the same station.
  STOP_MISMATCH,
  // it's not possible to resolve the stop reference (id or sequence) to the position in the pattern
  INVALID_STOP_REFERENCE,
  NO_SERVICE_ON_DATE,
  // an extra/added trip has a service date which is before or after any schedule data
  OUTSIDE_SERVICE_PERIOD,
  INVALID_ARRIVAL_TIME,
  INVALID_DEPARTURE_TIME,
  NEGATIVE_DWELL_TIME,
  NEGATIVE_HOP_TIME,
  INVALID_STOP_SEQUENCE,
  NOT_IMPLEMENTED_UNSCHEDULED,
  NOT_IMPLEMENTED_DUPLICATED,
  NOT_MONITORED,
  CANNOT_RESOLVE_AGENCY,
}
