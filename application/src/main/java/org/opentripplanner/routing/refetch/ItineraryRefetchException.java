package org.opentripplanner.routing.refetch;

/**
 * Thrown when an itinerary cannot be refetched, for example because a referenced trip no longer
 * exists, or because no access, egress or transfer can be found. The GTFS API then returns
 * {@code null}, the Transmodel API an invalid-input error.
 */
public class ItineraryRefetchException extends RuntimeException {

  public ItineraryRefetchException(String message) {
    super(message);
  }
}
