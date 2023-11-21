import { useCallback, useEffect, useState } from 'react';
import { graphql } from '../gql';
import request from 'graphql-request';
import { QueryType, TripQueryVariables } from '../gql/graphql.ts';

const endpoint = import.meta.env.VITE_API_URL;

const query = graphql(`
  query serverInfo {
    serverInfo {
      version
      otpSerializationVersionId
      buildConfigVersion
      routerConfigVersion
      gitCommit
      gitBranch
    }
  }
`);

export const useServerInfo = () => {
  const [data, setData] = useState<QueryType | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      console.log('i fire once');

      setData((await request(endpoint, query)) as QueryType);
    };
    fetchData();
  }, []);

  return data?.serverInfo;
};
