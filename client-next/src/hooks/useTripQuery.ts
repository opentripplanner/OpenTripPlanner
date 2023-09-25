import { useCallback, useState } from 'react';
import { graphql } from '../gql';
import request from 'graphql-request';
import { TripQuery, TripQueryVariables } from '../gql/graphql.ts';

// TODO: make this endpoint url configurable
const endpoint = `https://api.entur.io/journey-planner/v3/graphql`;

/**
  General purpose trip query document for debugging trip searches
  TODO: should live in a separate file, and split into fragments for readability
 */
const query = graphql(`
  query trip($from: Location!, $to: Location!, $arriveBy: Boolean) {
    trip(from: $from, to: $to, arriveBy: $arriveBy) {
      tripPatterns {
        aimedStartTime
        aimedEndTime
        duration
        distance
        legs {
          id
          mode
          pointsOnLink {
            points
          }
        }
      }
    }
  }
`);

type TripQueryHook = (variables?: TripQueryVariables) => [TripQuery | null, () => Promise<void>];

export const useTripQuery: TripQueryHook = (variables) => {
  const [data, setData] = useState<TripQuery | null>(null);
  const callback = useCallback(async () => {
    if (variables) {
      setData(await request(endpoint, query, variables));
    } else {
      console.warn("Can't search without variables");
    }
  }, [setData, variables]);
  return [data, callback];
};
