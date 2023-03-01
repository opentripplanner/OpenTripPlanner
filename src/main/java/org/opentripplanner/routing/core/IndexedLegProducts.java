package org.opentripplanner.routing.core;

import java.util.Collection;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.model.plan.Leg;

public record IndexedLegProducts(Collection<FareProduct> products, Leg leg, int legIndex) {}
