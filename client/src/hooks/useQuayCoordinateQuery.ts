import { useEffect, useState } from 'react';
import { request } from 'graphql-request'; // eslint-disable-line import/no-unresolved
import { Location, QueryType } from '../gql/graphql.ts';
import { getApiUrl } from '../util/getApiUrl.ts';
import { graphql } from '../gql';

const query = graphql(`
  query quayCoordinate($id: String!) {
    quay(id: $id) {
      longitude
      latitude
    }
  }
`);

export const useQuayCoordinateQuery = (location: Location) => {
  const [data, setData] = useState<QueryType | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      if (location.place) {
        const variables = { id: location.place };
        setData((await request(getApiUrl(), query, variables)) as QueryType);
      } else {
        setData(null);
      }
    };
    fetchData();
  }, [location]);

  return data?.quay;
};
