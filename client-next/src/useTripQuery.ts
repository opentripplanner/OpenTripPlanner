import { useCallback, useState } from 'react';
import { graphql } from './gql';
import request from 'graphql-request';
import { TripQuery, TripQueryVariables } from './gql/graphql.ts';

// TODO: make this endpoint url configurable
const endpoint = `https://api.entur.io/journey-planner/v3/graphql`;

/**
  General purpose trip query document for debugging trip searches
  TODO: should live in a separate file, and split into fragments for readability
 */
const query = graphql(`
  query trip {
    trip(from: { place: "NSR:StopPlace:337" }, to: { place: "NSR:StopPlace:1" }) {
      tripPatterns {
        legs {
          id
        }
      }
    }
  }
`);

type TripQueryHook = (variables?: TripQueryVariables) => [TripQuery | null, () => Promise<void>];

export const useTripQuery: TripQueryHook = (variables = {}) => {
  const [data, setData] = useState<TripQuery | null>(null);
  const callback = useCallback(async () => setData(await request(endpoint, query, variables)), [setData, variables]);
  return [data, callback];
};
