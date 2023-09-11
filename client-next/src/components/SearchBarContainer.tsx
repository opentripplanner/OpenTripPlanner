import { useTripQuery } from '../hooks/useTripQuery.ts';

export function SearchBarContainer() {
  const [data, callback] = useTripQuery();

  return (
    <>
      <button onClick={() => callback()}>Make request</button>
      {data && <pre>{JSON.stringify(data.trip.tripPatterns, null, 2)}</pre>}
    </>
  );
}
