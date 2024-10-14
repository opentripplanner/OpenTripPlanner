import { Stack } from 'react-bootstrap';
import { MapView } from '../components/MapView/MapView.tsx';
import { SearchBar } from '../components/SearchBar/SearchBar.tsx';
import { ItineraryListContainer } from '../components/ItineraryList/ItineraryListContainer.tsx';
import { useState } from 'react';
import { useTripQuery } from '../hooks/useTripQuery.ts';
import { useServerInfo } from '../hooks/useServerInfo.ts';
import { useTripQueryVariables } from '../hooks/useTripQueryVariables.ts';
import { TimeZoneContext } from '../hooks/TimeZoneContext.ts';

export function App() {
  const serverInfo = useServerInfo();
  const { tripQueryVariables, setTripQueryVariables } = useTripQueryVariables();
  const [tripQueryResult, loading, callback] = useTripQuery(tripQueryVariables);
  const [selectedTripPatternIndex, setSelectedTripPatternIndex] = useState<number>(0);
  const timeZone = serverInfo?.internalTransitModelTimeZone || Intl.DateTimeFormat().resolvedOptions().timeZone;

  return (
    <div className="app">
      <TimeZoneContext.Provider value={timeZone}>
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
      </TimeZoneContext.Provider>
    </div>
  );
}
