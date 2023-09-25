import { Button, Stack } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { LocationInputField } from './LocationInputField.tsx';
import { DepartureArrivalSelect } from './DepartureArrivalSelect.tsx';

export function SearchBar({
  onRoute,
  tripQueryVariables,
  setTripQueryVariables,
}: {
  onRoute: () => void;
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <section
      style={{
        height: '5rem',
        paddingLeft: '1rem',
      }}
    >
      <Stack direction="horizontal" gap={2}>
        <LocationInputField location={tripQueryVariables.from} label="From" id="fromInputField" />
        <LocationInputField location={tripQueryVariables.to} label="To" id="toInputField" />
        <DepartureArrivalSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <Button variant="primary" onClick={onRoute}>
          Route
        </Button>
      </Stack>
    </section>
  );
}
