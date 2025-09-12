import { TripQuery, Mode } from '../../gql/graphql.ts';
import { formatTime } from '../../util/formatTime.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { formatDistance } from '../../util/formatDistance.ts';

// Extract types from TripQuery for better type safety
export type TripPatternType = NonNullable<TripQuery['trip']['tripPatterns'][0]>;
export type LegType = NonNullable<TripPatternType['legs'][0]>;

export interface ItinerarySummary {
  startTime: string;
  endTime: string;
  duration: string;
  generalizedCost: number | null | undefined;
  walkingDistance: string;
  totalLegs: number;
}

export interface LegOption {
  value: string;
  label: string;
}

// Helper function to convert distance strings to meters for comparison
export const parseDistanceToMeters = (distanceStr: string): number => {
  const numericValue = parseFloat(distanceStr.replace(/[^0-9.]/g, ''));
  if (distanceStr.includes('km')) {
    return numericValue * 1000; // Convert km to meters
  }
  return numericValue; // Already in meters
};

// Configurable comparison logic for each column
export const getComparisonClass = (
  value1: number | null | undefined,
  value2: number | null | undefined,
  comparison: 'lower' | 'higher' = 'lower',
) => {
  if (value1 == null || value2 == null || value1 === value2) return '';
  const isBest = comparison === 'lower' ? value1 < value2 : value1 > value2;
  return isBest ? 'compare-value-best' : '';
};

// Column-specific comparison configurations
export const columnComparisons = {
  duration: 'lower' as const, // Lower duration is better
  cost: 'lower' as const, // Lower cost is better
  legs: 'lower' as const, // Fewer transfers is better
  walkingDistance: 'lower' as const, // Less walking is better
};

export const getItinerarySummary = (tripPattern: TripPatternType, timeZone: string): ItinerarySummary => {
  const startTime = formatTime(tripPattern.expectedStartTime, timeZone, 'short');
  const endTime = formatTime(tripPattern.expectedEndTime, timeZone, 'short');
  const duration = formatDuration(tripPattern.duration);
  const generalizedCost = tripPattern.generalizedCost;

  const walkingDistance = tripPattern.legs
    .filter((leg: LegType) => leg.mode === Mode.Foot)
    .reduce((sum: number, leg: LegType) => sum + leg.distance, 0);

  const totalLegs = Math.max(0, tripPattern.legs.length - 1); // Transfers = legs - 1, minimum 0

  return {
    startTime,
    endTime,
    duration,
    generalizedCost,
    walkingDistance: formatDistance(walkingDistance),
    totalLegs,
  };
};

// Create options for react-select dropdowns
export const createLegOptions = (legs: LegType[], itineraryNumber: number, timeZone: string): LegOption[] => {
  return legs.map((leg, index) => {
    const legId = leg.id || `leg${itineraryNumber}_${index}`;
    const label = `${leg.mode}${leg.line?.publicCode ? ` (${leg.line.publicCode})` : ''} - ${formatTime(leg.aimedStartTime, timeZone, 'short')} to ${formatTime(leg.aimedEndTime, timeZone, 'short')}`;
    return {
      value: legId,
      label: label,
    };
  });
};