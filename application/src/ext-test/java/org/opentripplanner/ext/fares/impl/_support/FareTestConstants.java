package org.opentripplanner.ext.fares.impl._support;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;

public interface FareTestConstants {
  FareProduct FARE_PRODUCT = FareProduct.of(id("fp"), "fare product 1", Money.euros(10)).build();

  FareProduct ONE_EUR_TRANSFER = FareProduct.of(
    id("transfer"),
    "transfer 1",
    Money.euros(1)
  ).build();
}
