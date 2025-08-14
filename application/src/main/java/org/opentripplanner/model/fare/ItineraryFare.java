package org.opentripplanner.model.fare;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 *
 * ItineraryFares is a set of fares for different legs, rider categories or fare media.
 */
@Sandbox
public class ItineraryFare {

  /**
   * The fare products that are valid for all legs of an itinerary, like a day pass.
   * <p>
   * Note: LinkedHashSet keeps the insertion order
   */
  private final Set<FareProduct> itineraryProducts = new LinkedHashSet<>();

  /**
   * Fare products that apply to one or more legs but _not_ the entire journey.
   * <p>
   * Note: LinkedHashMultimap keeps the insertion order
   */
  private final Multimap<Leg, FareOffer> legProducts = LinkedHashMultimap.create();

  public static ItineraryFare empty() {
    return new ItineraryFare();
  }

  /**
   * Get those fare products that cover the entire itinerary.
   */
  public List<FareProduct> getItineraryProducts() {
    return List.copyOf(itineraryProducts);
  }

  /**
   * Get those fare products that are valid for a subset of legs but not the entire itinerary.
   */
  public Multimap<Leg, FareOffer> getLegProducts() {
    return ImmutableMultimap.copyOf(legProducts);
  }

  /**
   * Add fare products that cover the entire itinerary, i.e. are valid for all legs.
   */
  public void addItineraryProducts(Collection<FareProduct> products) {
    itineraryProducts.addAll(products);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itineraryProducts, legProducts);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ItineraryFare fare1 = (ItineraryFare) o;
    return (
      Objects.equals(itineraryProducts, fare1.itineraryProducts) &&
      Objects.equals(legProducts, fare1.legProducts)
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addObj("itineraryProducts", itineraryProducts)
      .addObj("legProducts", legProducts)
      .toString();
  }

  /**
   * Add a single fare product for a single leg.
   */
  public void addFareProduct(Leg leg, FareOffer offer) {
    this.legProducts.put(leg, offer);
  }

  public void addFareProductUses(Multimap<Leg, FareOffer> fareProducts) {
    legProducts.putAll(fareProducts);
  }

  /**
   * Add the contents of another instance to this one.
   */
  public void add(ItineraryFare fare) {
    itineraryProducts.addAll(fare.itineraryProducts);
    legProducts.putAll(fare.legProducts);
  }

  /**
   * Does this instance contain any fare products?
   */
  public boolean isEmpty() {
    return itineraryProducts.isEmpty() && legProducts.isEmpty();
  }
}
