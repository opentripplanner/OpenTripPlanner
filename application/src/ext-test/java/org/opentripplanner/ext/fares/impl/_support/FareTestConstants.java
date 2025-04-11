package org.opentripplanner.ext.fares.impl._support;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;

public interface FareTestConstants {
  FareProduct FARE_PRODUCT = FareProduct.of(id("fp"), "fare product", Money.euros(10.00f)).build();

  FareProduct ONE_EUR_TRANSFER = FareProduct.of(id("transfer"), "transfer", Money.euros(1)).build();
}
