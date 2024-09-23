import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { ChangeEvent, useCallback, useContext, useMemo } from 'react';
import { Temporal } from '@js-temporal/polyfill';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';

export function DateTimeInputField({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const timeZone = useContext(TimeZoneContext);
  const current = useMemo(
    () =>
      Temporal.Instant.from(tripQueryVariables.dateTime)
        .toZonedDateTimeISO(timeZone)
        .toPlainDateTime()
        .toString({ smallestUnit: 'minute', calendarName: 'never' }),
    [tripQueryVariables.dateTime],
  );

  const onChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const dateTime = Temporal.PlainDateTime.from(event.target.value)
        .toZonedDateTime(timeZone)
        .toString({ calendarName: 'never', timeZoneName: 'never' });

      setTripQueryVariables({
        ...tripQueryVariables,
        dateTime: dateTime,
      });
    },
    [tripQueryVariables, setTripQueryVariables],
  );

  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="timePicker" title={'Time zone: ' + timeZone}>
        Time
      </Form.Label>
      <Form.Control type="datetime-local" id="timePicker" size="sm" onChange={onChange} value={current} />
    </Form.Group>
  );
}
