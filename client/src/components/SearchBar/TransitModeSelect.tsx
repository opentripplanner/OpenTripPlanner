import { TransportMode, TripQueryVariables } from '../../gql/graphql.ts';
import MultiSelectDropdown from './MultiSelectDropdown.tsx';
import { useCallback, useMemo } from 'react';

export function TransitModeSelect({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const values = useMemo(() => {
    return (
      tripQueryVariables?.modes?.transportModes
        ?.map((transportMode) => transportMode?.transportMode)
        .filter((v) => !!v) || []
    );
  }, [tripQueryVariables.modes?.transportModes]);

  // An empty list of transit modes means transit is disabled. This is documented in the API. If the list
  // contains an empty object (one without transportMode set), the API also disables transit, but this is
  // not a documented part of the API. In any case, both have the effect of disabling transit, and we can
  // check for both by checking if every element satisfies the condition `!it?.transportMode`.
  const transitDisabled =
    tripQueryVariables.modes?.transportModes != null &&
    tripQueryVariables.modes?.transportModes.every((it) => !it?.transportMode);

  const onChange = useCallback(
    (values: (TransportMode | null | undefined)[]) => {
      const newTransportModes = values
        .filter((v) => v != null)
        .map((v) => ({
          transportMode: v,
        }));

      setTripQueryVariables({
        ...tripQueryVariables,
        modes: {
          ...tripQueryVariables.modes,
          // Remove transportModes entirely when empty
          transportModes: newTransportModes.length === 0 ? undefined : newTransportModes,
        },
      });
    },
    [tripQueryVariables, setTripQueryVariables],
  );

  return (
    <MultiSelectDropdown
      label="Transit mode"
      emptySelectionText={transitDisabled ? 'None' : 'All'}
      options={Object.values(TransportMode).map((mode) => ({
        id: mode,
        label: mode.toString(),
      }))}
      values={values}
      onChange={onChange}
    />
  );
}
