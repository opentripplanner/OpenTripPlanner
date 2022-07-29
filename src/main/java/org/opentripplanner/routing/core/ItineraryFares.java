package org.opentripplanner.routing.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * <p>
 * Fare is a set of fares for different classes of users.
 * </p>
 */
public class ItineraryFares {

  /**
   * A mapping from {@link FareType} to a list of {@link FareComponent}. The FareComponents are
   * stored in an array instead of a list because JAXB doesn't know how to deal with interfaces when
   * serializing a trip planning response, and List is an interface. See
   * https://stackoverflow.com/a/1119241/778449
   */
  private final HashMap<FareType, List<FareComponent>> details = new HashMap<>();
  private final List<FareProduct> productsCoveringItinerary = new ArrayList<>();
  private final Multimap<Leg, FareProduct> legProducts = ArrayListMultimap.create();
  /**
   * A mapping from {@link FareType} to {@link Money}.
   */
  private HashMap<FareType, Money> fare = new HashMap<>();

  public ItineraryFares(ItineraryFares aFare) {
    if (aFare != null) {
      fare.putAll(aFare.fare);
    }
  }

  public ItineraryFares() {}

  public static ItineraryFares empty() {
    return new ItineraryFares();
  }

  public List<FareProduct> getProductsCoveringItinerary() {
    return List.copyOf(productsCoveringItinerary);
  }

  public Multimap<Leg, FareProduct> getLegProducts() {
    return ImmutableMultimap.copyOf(legProducts);
  }

  public void addFare(FareType fareType, Money money) {
    fare.put(fareType, money);
  }

  public void addFareDetails(FareType fareType, List<FareComponent> newDetails) {
    details.put(fareType, newDetails);
  }

  public void addProductsCoveringItinerary(List<FareProduct> products) {
    productsCoveringItinerary.addAll(products);
  }

  public Money getFare(FareType type) {
    return fare.get(type);
  }

  public List<FareComponent> getDetails(FareType type) {
    return details.get(type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(details);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ItineraryFares fare1 = (ItineraryFares) o;
    return Objects.equals(details, fare1.details);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("details", details).toString();
  }

  public Set<FareType> getTypes() {
    return fare.keySet();
  }

  public void addLegProducts(List<LegProducts> legProducts) {
    legProducts.forEach(lp -> this.legProducts.putAll(lp.leg(), lp.products()));
  }

  public void updateAllCurrencies(Currency newCurrency) {}

  public enum FareType implements Serializable {
    regular,
    student,
    senior,
    tram,
    special,
    youth,
  }
}
