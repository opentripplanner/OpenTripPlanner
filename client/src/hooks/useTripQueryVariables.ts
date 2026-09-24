import { useEffect, useState } from 'react';
import { Modes, StreetMode, TripQueryVariables } from '../gql/graphql.ts';

/**
 * These defaults correspond to the API default: foot.
 */
const DEFAULT_MODES: Modes = {
  accessMode: StreetMode.Foot,
  directMode: StreetMode.Foot,
  egressMode: StreetMode.Foot,
};

const DEFAULT_VARIABLES: TripQueryVariables = {
  from: {},
  to: {},
  dateTime: new Date().toISOString(),
  modes: DEFAULT_MODES,
};

const getInitialVariables = () => {
  const urlParams = new URLSearchParams(window.location.search);
  const variablesJson = urlParams.get('variables');
  if (!variablesJson) {
    return DEFAULT_VARIABLES;
  }
  const variables: TripQueryVariables = JSON.parse(decodeURIComponent(variablesJson));

  // A query without the `modes` element means the API defaults to foot as access, egress and direct mode.
  // But if we allow `modes` to be null, we need to handle extra states here in the frontend, so to
  // simplify, we instead set it to `DEFAULT_MODES`, causing the same behavior as omitting `modes`.
  return variables.modes ? variables : { ...variables, modes: DEFAULT_MODES };
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
