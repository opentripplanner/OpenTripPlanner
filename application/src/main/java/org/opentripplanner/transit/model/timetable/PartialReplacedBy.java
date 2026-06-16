package org.opentripplanner.transit.model.timetable;

/// A range of stops in a trip that are replacement by a TripOnServiceDate. fromPos and toPos are both
/// inclusive.
public record PartialReplacedBy(int fromPos, int toPos, TripOnServiceDate replacedBy) {}
