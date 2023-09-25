import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';

export function DepartureArrivalSelect({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const onChange = (arriveBy: boolean) => {
    setTripQueryVariables({
      ...tripQueryVariables,
      arriveBy,
    });
  };

  return (
    <Form.Group>
      <Form.Label htmlFor="departureArrivalSelect">Departure/Arrival</Form.Label>
      <Form.Select
        onChange={(e) => (e.target.value === 'arrival' ? onChange(true) : onChange(false))}
        value={tripQueryVariables.arriveBy ? 'arrival' : 'departure'}
      >
        <option value="arrival">Arrival</option>
        <option value="departure">Departure</option>
      </Form.Select>
    </Form.Group>
  );
}
