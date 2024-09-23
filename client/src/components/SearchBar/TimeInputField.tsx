import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { ChangeEvent, useCallback, useMemo } from 'react';
import { Temporal } from '@js-temporal/polyfill';
import { useTimeZone } from '../../hooks/useTimeZone.ts';

export function TimeInputField({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const current = useMemo(
    () =>
      Temporal.Instant.from(tripQueryVariables.dateTime)
        .toZonedDateTime({
          calendar: 'gregory',
          timeZone: useTimeZone(),
        })
        .toPlainDateTime()
        .toString({ smallestUnit: 'minute', calendarName: 'never' }),
    [tripQueryVariables.dateTime],
  );

  const onChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const dateTime = Temporal.PlainDateTime.from(event.target.value)
        .toZonedDateTime(useTimeZone())
        .toString({ calendarName: 'never', timeZoneName: 'never' });

      console.log(dateTime);
      setTripQueryVariables({
        ...tripQueryVariables,
        dateTime: dateTime,
      });
    },
    [tripQueryVariables, setTripQueryVariables],
  );

  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="timePicker" title={useTimeZone()}>
        Time
      </Form.Label>
      <Form.Control type="datetime-local" id="timePicker" size="sm" onChange={onChange} value={current} />
    </Form.Group>
  );
}
