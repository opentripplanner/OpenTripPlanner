package org.opentripplanner.model.plan;

import org.opentripplanner.ext.fares.model.FareProduct;

public record FareProductInstance(String instanceId, FareProduct product) {}
