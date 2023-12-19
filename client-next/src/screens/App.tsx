import { Stack } from 'react-bootstrap';
import { MapView } from '../components/MapView/MapView.tsx';
import { SearchBar } from '../components/SearchBar/SearchBar.tsx';
import { ItineraryListContainer } from '../components/ItineraryList/ItineraryListContainer.tsx';
import { useState } from 'react';
import { TripQueryVariables } from '../gql/graphql.ts';
import { useTripQuery } from '../hooks/useTripQuery.ts';
import { useServerInfo } from '../hooks/useServerInfo.ts';

const INITIAL_VARIABLES: TripQueryVariables = {
  from: {},
  to: {},
  dateTime: new Date().toISOString(),
};

export function App() {
  const [tripQueryVariables, setTripQueryVariables] = useState<TripQueryVariables>(INITIAL_VARIABLES);
  const [tripQueryResult, loading, callback] = useTripQuery(tripQueryVariables);
  const serverInfo = useServerInfo();
  const [selectedTripPatternIndex, setSelectedTripPatternIndex] = useState<number>(0);

  return (
    <div className="app">
      <SearchBar
        onRoute={callback}
        tripQueryVariables={tripQueryVariables}
        setTripQueryVariables={setTripQueryVariables}
        serverInfo={serverInfo}
        loading={loading}
      />

      <Stack direction="horizontal" gap={0}>
        <ItineraryListContainer
          tripQueryResult={tripQueryResult}
          selectedTripPatternIndex={selectedTripPatternIndex}
          setSelectedTripPatternIndex={setSelectedTripPatternIndex}
          pageResults={callback}
          loading={loading}
        />
        <MapView
          tripQueryResult={tripQueryResult}
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
          selectedTripPatternIndex={selectedTripPatternIndex}
          loading={loading}
        />
      </Stack>
    </div>
  );
}
