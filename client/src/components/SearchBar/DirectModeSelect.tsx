import { Form } from 'react-bootstrap';
import { StreetMode, TripQueryVariables } from '../../gql/graphql.ts';

export function DirectModeSelect({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="directModeSelect">
        Direct mode
      </Form.Label>
      <Form.Select
        id="directModeSelect"
        size="sm"
        className="input-medium"
        onChange={(e) => {
          setTripQueryVariables({
            ...tripQueryVariables,
            modes: {
              ...tripQueryVariables.modes,
              directMode: e.target.value === 'not_selected' ? undefined : (e.target.value as StreetMode),
            },
          });
        }}
        value={tripQueryVariables.modes?.directMode || 'not_selected'}
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
