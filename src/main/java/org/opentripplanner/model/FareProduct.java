package org.opentripplanner.model;

import org.opentripplanner.routing.core.Money;

public record FareProduct(String id, String name, Money amount) {}
