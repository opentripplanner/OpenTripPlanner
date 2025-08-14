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
          if (e.target.value !== 'not_selected') {
            setTripQueryVariables({
              ...tripQueryVariables,
              modes: {
                ...tripQueryVariables.modes,
                egressMode: e.target.value as StreetMode,
              },
            });
          } else {
            setTripQueryVariables({
              ...tripQueryVariables,
              modes:
                tripQueryVariables.modes?.directMode || tripQueryVariables.modes?.transportModes
                  ? {
                      ...tripQueryVariables.modes,
                      egressMode: undefined,
                    }
                  : undefined,
            });
          }
        }}
        value={tripQueryVariables.modes?.egressMode || 'not_selected'}
      >
        <option value="not_selected">Not selected</option>
        {Object.values(StreetMode).map((mode) => (
          <option key={mode} value={mode}>
            {mode}
          </option>
        ))}
      </Form.Select>
    </Form.Group>
  );
}
