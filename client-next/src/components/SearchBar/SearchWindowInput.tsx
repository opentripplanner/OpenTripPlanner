import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';

export function SearchWindowInput({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <Form.Group>
      <Form.Label htmlFor="searchWindowInput">Search window (seconds)</Form.Label>
      <Form.Control
        type="number"
        id="searchWindowInput"
        size="sm"
        placeholder="3600"
        value={tripQueryVariables.searchWindow || undefined}
        onChange={(event) =>
          setTripQueryVariables({
            ...tripQueryVariables,
            searchWindow: Number(event.target.value),
          })
        }
      />
    </Form.Group>
  );
}
