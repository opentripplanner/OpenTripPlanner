import { useCallback, useEffect, useState } from 'react';
import { graphql } from '../gql';
import request from 'graphql-request';
import { QueryType, TripQueryVariables } from '../gql/graphql.ts';

const endpoint = import.meta.env.VITE_API_URL;

/**
  General purpose trip query document for debugging trip searches
  TODO: should live in a separate file, and split into fragments for readability
 */
const query = graphql(`
  query trip(
    $from: Location!
    $to: Location!
    $arriveBy: Boolean
    $dateTime: DateTime
    $numTripPatterns: Int
    $searchWindow: Int
    $modes: Modes
    $itineraryFiltersDebug: ItineraryFilterDebugProfile
    $pageCursor: String
  ) {
    trip(
      from: $from
      to: $to
      arriveBy: $arriveBy
      dateTime: $dateTime
      numTripPatterns: $numTripPatterns
      searchWindow: $searchWindow
      modes: $modes
      itineraryFilters: { debug: $itineraryFiltersDebug }
      pageCursor: $pageCursor
    ) {
      previousPageCursor
      nextPageCursor
      tripPatterns {
        aimedStartTime
        aimedEndTime
        expectedEndTime
        expectedStartTime
        duration
        distance
        legs {
          id
          mode
          aimedStartTime
          aimedEndTime
          expectedEndTime
          expectedStartTime
          realtime
          distance
          duration
          fromPlace {
            name
          }
          toPlace {
            name
          }
          toEstimatedCall {
            destinationDisplay {
              frontText
            }
          }
          line {
            publicCode
            name
          }
          authority {
            name
          }
          pointsOnLink {
            points
          }
        }
        systemNotices {
          tag
        }
      }
    }
  }
`);

type TripQueryHook = (
  variables?: TripQueryVariables,
) => [QueryType | null, boolean, (pageCursor?: string) => Promise<void>];

export const useTripQuery: TripQueryHook = (variables) => {
  const [data, setData] = useState<QueryType | null>(null);
  const [loading, setLoading] = useState(false);
  const callback = useCallback(
    async (pageCursor?: string) => {
      if (loading) {
        console.warn('Wait for previous search to finish');
      } else {
        if (variables) {
          setLoading(true);
          if (pageCursor) {
            setData((await request(endpoint, query, { ...variables, pageCursor })) as QueryType);
          } else {
            setData((await request(endpoint, query, variables)) as QueryType);
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
    if (variables?.from.coordinates && variables?.to.coordinates) {
      callback();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [variables?.from, variables?.to]);

  return [data, loading, callback];
};
