package org.opentripplanner.ext.fares.impl.gtfs;

import java.util.Collection;
import org.opentripplanner.model.fare.FareProduct;

public record TransferFareProduct(
  FareProduct transferProduct,
  Collection<FareProduct> dependencies
) {}
