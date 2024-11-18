import { useEffect, useState } from 'react';
import { request } from 'graphql-request';
import { Location, QueryType } from '../gql/graphql.ts';
import { getApiUrl } from '../util/getApiUrl.ts';
import { graphql } from '../gql';

const query = graphql(`
  query quayCoordinate($id: String!) {
    quay(id: $id) {
      latitude
      longitude
    }
  }
`);

export const useQuayCoordinateQuery = (location: Location) => {
  const [data, setData] = useState<QueryType | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      if (location.place) {
        const variables = { id: location.place };
        try {
          setData((await request(getApiUrl(), query, variables)) as QueryType);
        } catch (e) {
          console.error('Error at useQuayCoordinateQuery', e);
        }
      } else {
        setData(null);
      }
    };
    fetchData();
  }, [location]);

  return data?.quay;
};
