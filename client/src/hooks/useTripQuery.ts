import { useCallback, useEffect, useState } from 'react';
import { request } from 'graphql-request';
import { Location, TripQuery, TripQueryVariables } from '../gql/graphql.ts';
import { getApiUrl } from '../util/getApiUrl.ts';
import { createPrunedQuery, createPrunedVariables } from '../util/queryPruning.ts';

/**
  General purpose trip query document for debugging trip searches
 */

type TripQueryHook = (
  variables?: TripQueryVariables,
) => [TripQuery | null, boolean, (pageCursor?: string) => Promise<void>, unknown];

export const useTripQuery: TripQueryHook = (variables) => {
  const [data, setData] = useState<TripQuery | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const callback = useCallback(
    async (pageCursor?: string) => {
      if (loading) {
        console.warn('Wait for previous search to finish');
      } else {
        if (variables) {
          setLoading(true);
          setError(null);
          try {
            // Create pruned variables and query for the API call
            const baseVariables = pageCursor ? { ...variables, pageCursor } : variables;
            const prunedVariables = createPrunedVariables(baseVariables);
            const prunedQuery = createPrunedQuery(baseVariables);

            setData((await request(getApiUrl(), prunedQuery, prunedVariables)) as TripQuery);
          } catch (e) {
            console.error('Error at useTripQuery', e);
            setError(e);
            setData(null);
          }
          setLoading(false);
        } else {
          console.warn("Can't search without variables");
        }
      }
    },
    [setData, variables, loading],
  );

  useEffect(() => {
    if (validLocation(variables?.from) && validLocation(variables?.to)) {
      callback();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [variables?.from, variables?.to]);
  return [data, loading, callback, error];
};

function validLocation(location: Location | undefined) {
  return location && (location.coordinates || location.place);
}
