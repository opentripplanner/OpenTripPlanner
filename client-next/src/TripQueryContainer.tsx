import {useTripQuery} from "./useTripQuery.ts";

export function TripQueryContainer() {
  const [data, callback] = useTripQuery();

  return (
    <>
      <button onClick={() => callback()}>Make request</button>
      {data && (
        <pre>
          {JSON.stringify(data.trip.tripPatterns, null, 2)}
        </pre>
      )}

    </>
  );
}