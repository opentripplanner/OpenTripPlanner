import { Form } from 'react-bootstrap';
import { ItineraryFilterDebugProfile, TripQueryVariables } from '../../gql/graphql.ts';
import { mediumInputStyle } from '../ItineraryList/inputStyle.ts';

export function ItineraryFilterDebugSelect({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="itineraryDebugFilterSelect">
        Filter debug
      </Form.Label>
      <Form.Select
        id="itineraryDebugFilterSelect"
        size="sm"
        style={mediumInputStyle}
        onChange={(e) => {
          setTripQueryVariables({
            ...tripQueryVariables,
            itineraryFiltersDebug: e.target.value as ItineraryFilterDebugProfile,
          });
        }}
        value={tripQueryVariables.itineraryFiltersDebug || 'not_selected'}
      >
        <option value="not_selected">Not selected</option>
        {Object.values(ItineraryFilterDebugProfile).map((debugProfile) => (
          <option key={debugProfile} value={debugProfile}>
            {debugProfile}
          </option>
        ))}
      </Form.Select>
    </Form.Group>
  );
}
