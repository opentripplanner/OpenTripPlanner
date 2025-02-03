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
      <Form.Label column="sm" htmlFor="departureArrivalSelect">
        Departure/Arrival
      </Form.Label>
      <Form.Select
        size="sm"
        onChange={(e) => (e.target.value === 'arrival' ? onChange(true) : onChange(false))}
        value={tripQueryVariables.arriveBy ? 'arrival' : 'departure'}
        style={{ verticalAlign: 'bottom' }}
      >
        <option value="arrival">Arrive before</option>
        <option value="departure">Depart after</option>
      </Form.Select>
    </Form.Group>
  );
}
