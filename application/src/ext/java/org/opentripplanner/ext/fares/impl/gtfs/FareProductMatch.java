package org.opentripplanner.ext.fares.impl.gtfs;

import java.util.Set;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Leg;

record FareProductMatch(Leg leg, Set<FareProduct> fareProducts) {}
