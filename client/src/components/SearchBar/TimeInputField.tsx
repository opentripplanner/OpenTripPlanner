import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { ChangeEvent, useCallback, useMemo } from 'react';

export function TimeInputField({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const current = useMemo(
    () => new Date(tripQueryVariables.dateTime).toTimeString().split(' ')[0],
    [tripQueryVariables.dateTime],
  );

  const onChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const timeComponents = event.target.value.split(':');
      const newDate = new Date(tripQueryVariables.dateTime);
      newDate.setHours(Number(timeComponents[0]), Number(timeComponents[1]), Number(timeComponents[2]));

      setTripQueryVariables({
        ...tripQueryVariables,
        dateTime: newDate.toISOString(),
      });
    },
    [tripQueryVariables, setTripQueryVariables],
  );

  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="timePicker">
        Time
      </Form.Label>
      <Form.Control type="time" id="timePicker" size="sm" onChange={onChange} value={current} />
    </Form.Group>
  );
}
