import { useEffect, useState } from 'react';
import { TripQueryVariables } from '../gql/graphql.ts';

const DEFAULT_VARIABLES: TripQueryVariables = {
  from: {},
  to: {},
  dateTime: new Date().toISOString(),
};

const getInitialVariables = () => {
  const urlParams = new URLSearchParams(window.location.search);
  const variablesJson = urlParams.get('variables');
  return variablesJson ? JSON.parse(decodeURIComponent(variablesJson)) : DEFAULT_VARIABLES;
};

const updateUrlWithVariables = (variables: TripQueryVariables) => {
  const urlParams = new URLSearchParams(window.location.search);
  urlParams.set('variables', encodeURIComponent(JSON.stringify(variables)));
  history.pushState({}, '', '?' + urlParams.toString() + window.location.hash);
};

export const useTripQueryVariables = () => {
  const [tripQueryVariables, setTripQueryVariables] = useState<TripQueryVariables>(getInitialVariables());

  useEffect(() => {
    updateUrlWithVariables(tripQueryVariables);
  }, [tripQueryVariables]);

  return {
    tripQueryVariables,
    setTripQueryVariables,
  };
};
