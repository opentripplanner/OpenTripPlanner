package org.opentripplanner.api.model;

import java.util.Collection;
import java.util.List;

public record ApiLegProducts(List<Integer> legIndices, Collection<ApiFareProduct> products) {}
