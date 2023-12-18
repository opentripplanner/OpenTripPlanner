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

  if (hrs === 1) {
    formatted = `${formatted}${hrs} hr `;
  } else if (hrs > 1) {
    formatted = `${formatted}${hrs} hrs `;
  }

  if (mins === 1) {
    formatted = `${formatted}${mins} min `;
  } else if (mins > 1) {
    formatted = `${formatted}${mins} mins `;
  }

  if (secs === 1) {
    formatted = `${formatted}${secs} sec `;
  } else if (secs > 1) {
    formatted = `${formatted}${secs} secs `;
  }

  return formatted;
}
