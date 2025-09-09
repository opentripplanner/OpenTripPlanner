import { Form } from 'react-bootstrap';
import { ItineraryFilterDebugProfile, TripQueryVariables } from '../../gql/graphql.ts';

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
        className="input-medium"
        onChange={(e) => {
          if (e.target.value === 'not_selected') {
            // Remove the debug filter entirely when "Not selected" is chosen
            const updatedFilters = { ...tripQueryVariables.itineraryFilters };
            delete updatedFilters.debug;

            // If itineraryFilters becomes empty, remove it entirely
            const hasOtherFilters = Object.keys(updatedFilters).length > 0;

            setTripQueryVariables({
              ...tripQueryVariables,
              itineraryFilters: hasOtherFilters ? updatedFilters : undefined,
            });
          } else {
            setTripQueryVariables({
              ...tripQueryVariables,
              itineraryFilters: {
                ...tripQueryVariables.itineraryFilters,
                debug: e.target.value as ItineraryFilterDebugProfile,
              },
            });
          }
        }}
        value={tripQueryVariables.itineraryFilters?.debug || 'not_selected'}
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
