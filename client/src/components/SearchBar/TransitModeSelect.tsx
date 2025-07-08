import { StreetMode, TransportMode, TripQueryVariables } from '../../gql/graphql.ts';
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
      const newTransportModes = values.map((v) => ({
        transportMode: v,
      }));

      if (newTransportModes.length === 0) {
        setTripQueryVariables({
          ...tripQueryVariables,
          modes:
            tripQueryVariables.modes?.directMode ||
            tripQueryVariables.modes?.accessMode ||
            tripQueryVariables.modes?.egressMode
              ? { ...tripQueryVariables.modes }
              : undefined,
        });
      } else {
        const accessMode = tripQueryVariables.modes?.accessMode || StreetMode.Foot;
        const egressMode = tripQueryVariables.modes?.egressMode || StreetMode.Foot;
        setTripQueryVariables({
          ...tripQueryVariables,
          modes: {
            ...tripQueryVariables.modes,
            transportModes: newTransportModes.length > 0 ? newTransportModes : undefined,
            accessMode: accessMode,
            egressMode: egressMode,
          },
        });
      }
    },
    [tripQueryVariables, setTripQueryVariables],
  );

  return (
    <MultiSelectDropdown
      label="Transit mode"
      options={Object.values(TransportMode).map((mode) => ({
        id: mode,
        label: mode.toString(),
      }))}
      values={values}
      onChange={onChange}
    />
  );
}
