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
      emptySelectionText="All"
      options={Object.values(TransportMode).map((mode) => ({
        id: mode,
        label: mode.toString(),
      }))}
      values={values}
      onChange={onChange}
    />
  );
}
