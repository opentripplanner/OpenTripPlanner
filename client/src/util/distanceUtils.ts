/**
 * Distance utility functions for formatting and parsing distance values
 *
 * formatDistance adapted from src/client/js/otp/util/Geo.js#distanceStringMetric
 */

/**
 * Format distance in meters to human-readable format (m/km)
 * @param meters - Distance in meters
 * @returns Formatted distance string
 */
export function formatDistance(meters: number): string {
  const kilometers = meters / 1000;
  if (kilometers > 100) {
    //100 km => 999999999 km
    return `${kilometers.toFixed(0)} km`;
  } else if (kilometers > 1) {
    //1.1 km => 99.9 km
    return `${kilometers.toFixed(1)} km`;
  } else {
    //1m => 999m
    return `${meters.toFixed(0)} m`;
  }
}

/**
 * Parse formatted distance strings back to meters for comparison
 * @param distanceStr - Formatted distance string (e.g., "5.2 km", "150 m")
 * @returns Distance in meters
 */
export const parseDistanceToMeters = (distanceStr: string): number => {
  const numericValue = parseFloat(distanceStr.replace(/[^0-9.]/g, ''));
  if (distanceStr.includes('km')) {
    return numericValue * 1000; // Convert km to meters
  }
  return numericValue; // Already in meters
};
