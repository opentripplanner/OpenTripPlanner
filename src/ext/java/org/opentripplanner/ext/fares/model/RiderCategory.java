package org.opentripplanner.ext.fares.model;

import javax.annotation.Nullable;

public record RiderCategory(String id, String name, @Nullable String url) {}
