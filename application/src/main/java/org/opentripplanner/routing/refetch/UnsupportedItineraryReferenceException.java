package org.opentripplanner.routing.refetch;

/**
 * Thrown when an itinerary cannot be turned into an
 * {@link org.opentripplanner.model.plan.itineraryreference.ItineraryReference}, or when a decoded
 * reference holds something this version cannot use. The API then returns {@code null}.
 */
public class UnsupportedItineraryReferenceException extends RefetchItineraryException {

  public UnsupportedItineraryReferenceException(String message) {
    super(message);
  }
}
