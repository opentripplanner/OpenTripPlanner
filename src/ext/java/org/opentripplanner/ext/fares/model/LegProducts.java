package org.opentripplanner.ext.fares.model;

import java.util.Set;
import org.opentripplanner.model.plan.Leg;

public record LegProducts(Leg leg, Set<FareProduct> products) {}
