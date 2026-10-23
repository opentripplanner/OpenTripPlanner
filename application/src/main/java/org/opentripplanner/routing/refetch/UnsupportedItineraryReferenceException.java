package org.opentripplanner.routing.refetch;

/**
 * Thrown when an {@link org.opentripplanner.model.plan.Itinerary} cannot be represented as an
 * {@link org.opentripplanner.model.plan.itineraryreference.ItineraryReference} — for example
 * because one of its legs has no {@link org.opentripplanner.model.plan.legreference.LegReference}
 * — or when a decoded {@code ItineraryReference} turns out to hold a leg reference type
 * {@link ItineraryReferenceMapper} does not support.
 * <p>
 * Callers producing a client-facing itinerary ID should catch this and expose no ID (rather than
 * a partially-built one that cannot later be refetched), not a 500/internal error. It is a
 * {@link RefetchItineraryException} so that a caller refetching an itinerary from a reference only
 * has to catch one exception type for every way the reference can turn out not to be usable.
 */
public class UnsupportedItineraryReferenceException extends RefetchItineraryException {

  public UnsupportedItineraryReferenceException(String message) {
    super(message);
  }
}
