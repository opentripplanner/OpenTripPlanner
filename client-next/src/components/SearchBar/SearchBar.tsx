import { Button, Stack } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { LocationInputField } from './LocationInputField.tsx';

export function SearchBar({
  onRoute,
  tripQueryVariables,
}: {
  onRoute: () => void;
  tripQueryVariables?: TripQueryVariables;
}) {
  return (
    <section
      style={{
        height: '5rem',
        paddingLeft: '1rem',
      }}
    >
      <Stack direction="horizontal" gap={2}>
        <LocationInputField location={tripQueryVariables?.from} label="From" id="fromInputField" />
        <LocationInputField location={tripQueryVariables?.to} label="To" id="toInputField" />
        <Button variant="primary" onClick={onRoute}>
          Route
        </Button>
      </Stack>
    </section>
  );
}
