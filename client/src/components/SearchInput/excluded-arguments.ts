export const excludedArguments = new Set<string>([
  'numTripPatterns',
  'arriveBy',
  'from',
  'to',
  'dateTime',
  'searchWindow',
  'modes.accessMode',
  'modes.directMode',
  'modes.egressMode',
  // Add every full path you want to exclude - top level paths will remove all children!
]);
