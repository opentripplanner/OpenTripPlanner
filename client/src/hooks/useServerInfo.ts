import { useEffect, useState } from 'react';
import { graphql } from '../gql';
import { request } from 'graphql-request'; // eslint-disable-line import/no-unresolved
import { QueryType } from '../gql/graphql.ts';
import { getApiUrl } from '../util/getApiUrl.ts';

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
      setData((await request(getApiUrl(), query)) as QueryType);
    };
    fetchData();
  }, []);

  return data?.serverInfo;
};
