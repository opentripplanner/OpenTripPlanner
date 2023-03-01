package org.opentripplanner.routing.core;

import java.util.List;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.model.plan.Leg;

/**
 * The result of computing the indices of legs that are associated with fares.
 * @param products
 * @param leg The leg that the fare products are valid for.
 * @param legIndex The index of the leg in the itinerary that the fare products are valid for.
 */
public record IndexedLegProducts(List<FareProduct> products, Leg leg, int legIndex) {}
