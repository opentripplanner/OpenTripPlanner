import { Form } from 'react-bootstrap';
import { StreetMode, TripQueryVariables } from '../../gql/graphql.ts';

export function EgressSelect({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="egressSelect">
        Egress
      </Form.Label>
      <Form.Select
        id="egressSelect"
        size="sm"
        className="input-medium"
        onChange={(e) => {
          setTripQueryVariables({
            ...tripQueryVariables,
            modes: {
              ...tripQueryVariables.modes,
              egressMode: e.target.value === 'none' ? undefined : (e.target.value as StreetMode),
            },
          });
        }}
        value={tripQueryVariables.modes?.egressMode || 'none'}
      >
        <option value="none">None</option>
        {Object.values(StreetMode).map((mode) => (
          <option key={mode} value={mode}>
            {mode}
          </option>
        ))}
      </Form.Select>
    </Form.Group>
  );
}
