import { Mode } from '../../gql/graphql.ts';
import { formatTime } from '../../util/formatTime.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { formatDistance } from '../../util/distanceUtils.ts';
import { TripPattern, Leg } from '../../static/query/tripQueryTypes.js';

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

export const getComparisonClass = (
  value1: number | null | undefined,
  value2: number | null | undefined,
  comparison: 'lower' | 'higher' = 'lower',
) => {
  if (value1 == null || value2 == null || value1 === value2) return '';
  const isBest = comparison === 'lower' ? value1 < value2 : value1 > value2;
  return isBest ? 'compare-value-best' : '';
};

export const columnComparisons = {
  duration: 'lower' as const, // Lower duration is better
  cost: 'lower' as const, // Lower cost is better
  legs: 'lower' as const, // Fewer transfers is better
  walkingDistance: 'lower' as const, // Less walking is better
};

export const getItinerarySummary = (tripPattern: TripPattern, timeZone: string): ItinerarySummary => {
  if (!tripPattern) throw new Error('TripPattern is required');
  const startTime = formatTime(tripPattern.expectedStartTime, timeZone, 'short');
  const endTime = formatTime(tripPattern.expectedEndTime, timeZone, 'short');
  const duration = formatDuration(tripPattern.duration);
  const generalizedCost = tripPattern.generalizedCost;

  const walkingDistance = tripPattern.legs
    .filter((leg: Leg) => leg && leg.mode === Mode.Foot)
    .reduce((sum: number, leg: Leg) => sum + leg.distance, 0);

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

export const createLegOptions = (legs: Leg[], itineraryNumber: number, timeZone: string): LegOption[] => {
  return legs.map((leg, index) => {
    const legId = leg.id || `leg${itineraryNumber}_${index}`;
    const fromName = leg.fromPlace?.name || 'Unknown';
    const toName = leg.toPlace?.name || 'Unknown';
    const label = `${leg.mode}${leg.line?.publicCode ? ` (${leg.line.publicCode})` : ''} | ${fromName} → ${toName} | ${formatTime(leg.aimedStartTime, timeZone, 'short')} to ${formatTime(leg.aimedEndTime, timeZone, 'short')}`;
    return {
      value: legId,
      label: label,
    };
  });
};
