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
      <Form.Label column="sm" htmlFor="searchWindowInput">
        Search window
      </Form.Label>
      <Form.Control
        type="number"
        id="searchWindowInput"
        size="sm"
        placeholder="(in minutes)"
        min={1}
        value={tripQueryVariables.searchWindow || ''}
        onChange={(event) =>
          setTripQueryVariables({
            ...tripQueryVariables,
            searchWindow: Number(event.target.value) > 0 ? Number(event.target.value) : undefined,
          })
        }
      />
    </Form.Group>
  );
}
