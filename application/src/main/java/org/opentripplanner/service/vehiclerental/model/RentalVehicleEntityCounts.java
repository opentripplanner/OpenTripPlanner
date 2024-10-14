package org.opentripplanner.service.vehiclerental.model;

import java.util.List;

public record RentalVehicleEntityCounts(int total, List<RentalVehicleTypeCount> byType) {}
