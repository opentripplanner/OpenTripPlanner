import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { ChangeEvent, useCallback, useMemo } from 'react';

export function DateInputField({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const current = useMemo(
    () => new Date(tripQueryVariables.dateTime).toISOString().split('T')[0],
    [tripQueryVariables.dateTime],
  );

  const onChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const oldDate = new Date(tripQueryVariables.dateTime);
      const newDate = new Date(event.target.value);

      newDate.setHours(oldDate.getHours(), oldDate.getMinutes(), oldDate.getSeconds());

      setTripQueryVariables({
        ...tripQueryVariables,
        dateTime: newDate.toISOString(),
      });
    },
    [tripQueryVariables, setTripQueryVariables],
  );

  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="datePicker">
        Date
      </Form.Label>
      <Form.Control type="date" id="datePicker" size="sm" onChange={onChange} value={current} />
    </Form.Group>
  );
}
