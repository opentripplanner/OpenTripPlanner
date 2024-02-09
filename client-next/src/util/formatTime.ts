/**
 * Format departure and arrival times from scalar dateTime strings
 *
 * If style argument is provided formatted with ('medium') or without ('short') seconds,
 * otherwise seconds are shown if not 0.
 */
export function formatTime(dateTime: string, style?: 'short' | 'medium') {
  const parsed = new Date(dateTime);
  return parsed.toLocaleTimeString('en-US', {
    timeStyle: style ? style : parsed.getSeconds() === 0 ? 'short' : 'medium',
    hourCycle: 'h24',
  });
}
