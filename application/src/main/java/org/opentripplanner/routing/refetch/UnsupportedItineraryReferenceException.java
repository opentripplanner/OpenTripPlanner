package org.opentripplanner.routing.refetch;

/**
 * Thrown when an {@link org.opentripplanner.model.plan.Itinerary} cannot be represented as an
 * {@link org.opentripplanner.model.plan.itineraryreference.ItineraryReference} — for example
 * because one of its legs has no {@link org.opentripplanner.model.plan.legreference.LegReference}.
 * <p>
 * Callers producing a client-facing itinerary ID should catch this and expose no ID (rather than
 * a partially-built one that cannot later be refetched), not a 500/internal error.
 */
public class UnsupportedItineraryReferenceException extends RuntimeException {

  public UnsupportedItineraryReferenceException(String message) {
    super(message);
  }
}
