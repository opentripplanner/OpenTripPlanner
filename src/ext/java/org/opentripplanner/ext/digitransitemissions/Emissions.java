package org.opentripplanner.ext.digitransitemissions;

sealed interface Emissions permits DigitransitEmissions {
  double getEmissionsPerPassenger();
}
