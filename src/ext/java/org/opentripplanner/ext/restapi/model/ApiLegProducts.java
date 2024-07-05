package org.opentripplanner.ext.restapi.model;

import java.util.Collection;
import java.util.List;

/**
 *
 * @param legIndices The leg indices inside the itinerary that these products are valid for.
 * @param products The list of products that are valid for the leg referenced by the indices.
 */
public record ApiLegProducts(List<Integer> legIndices, Collection<ApiFareProduct> products) {}
