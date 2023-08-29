import {useJourneyPlannerRequest} from "./useJourneyPlannerRequest.ts";

export function JourneyPlannerRequestContainer() {
  const [data, callback] = useJourneyPlannerRequest();

  return (
    <>
      <button onClick={() => callback()}>Make request</button>
      {data && (
        <pre>
          {JSON.stringify(data, null, 2)}
        </pre>
      )}

    </>
  );
}