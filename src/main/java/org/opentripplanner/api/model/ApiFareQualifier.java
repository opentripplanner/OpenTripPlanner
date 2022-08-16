package org.opentripplanner.api.model;

/**
 * Qualifiers for Fares V2 fare products. Qualifiers can be rider categories (youth, senior, veteran) or
 * a fare container (smart card, app...).
 * @param id
 * @param name
 */
public record ApiFareQualifier(String id, String name) {}
