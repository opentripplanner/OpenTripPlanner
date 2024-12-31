/**
 * Format duration in seconds
 *
 * Adapted from src/client/js/otp/util/Time.js#secsToHrMinSec
 */
export function formatDuration(seconds: number) {
  const hrs = Math.floor(seconds / 3600);
  const mins = Math.floor(seconds / 60) % 60;
  const secs = seconds % 60;

  let formatted = '';

  if (hrs > 0) {
    formatted = `${formatted}${hrs}h `;
  }

  if (mins > 0) {
    formatted = `${formatted}${mins}min `;
  }

  if (secs > 1) {
    formatted = `${formatted}${secs}s`;
  }

  return formatted;
}
