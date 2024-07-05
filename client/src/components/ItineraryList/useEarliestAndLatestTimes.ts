import { useMemo } from 'react';
import { TripQuery } from '../../gql/graphql.ts';

export function useEarliestAndLatestTimes(tripQueryResult: TripQuery | null) {
  const earliestStartTime = useMemo(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.expectedStartTime;
        } else {
          return new Date(current?.expectedStartTime) < new Date(acc) ? current.expectedStartTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  const latestEndTime = useMemo<string | null>(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.expectedEndTime;
        } else {
          return new Date(current?.expectedEndTime) > new Date(acc) ? current.expectedEndTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  return [earliestStartTime, latestEndTime];
}
