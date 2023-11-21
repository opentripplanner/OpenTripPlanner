/**
 * Format distance
 *
 * Adapted from src/client/js/otp/util/Geo.js#distanceStringMetric
 */
export function formatDistance(meters: number) {
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
