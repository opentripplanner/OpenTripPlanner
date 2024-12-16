import { MapView } from '../components/MapView/MapView.tsx';
import { ItineraryListContainer } from '../components/ItineraryList/ItineraryListContainer.tsx';
import { useState } from 'react';
import { useTripQuery } from '../hooks/useTripQuery.ts';
import { useServerInfo } from '../hooks/useServerInfo.ts';
import { useTripQueryVariables } from '../hooks/useTripQueryVariables.ts';
import { TimeZoneContext } from '../hooks/TimeZoneContext.ts';
import { LogoSection } from '../components/SearchBar/LogoSection.tsx';
import { InputFieldsSection } from '../components/SearchBar/InputFieldsSection.tsx';

export function App() {
  const serverInfo = useServerInfo();
  const { tripQueryVariables, setTripQueryVariables } = useTripQueryVariables();
  const [tripQueryResult, loading, callback] = useTripQuery(tripQueryVariables);
  const [selectedTripPatternIndex, setSelectedTripPatternIndex] = useState<number>(0);
  const timeZone = serverInfo?.internalTransitModelTimeZone || Intl.DateTimeFormat().resolvedOptions().timeZone;

  return (
    <div className="app">
      <TimeZoneContext.Provider value={timeZone}>
        <div className="layout">
          <div className="box logo-section">
            <LogoSection serverInfo={serverInfo}></LogoSection>
          </div>
          <div className="box input-section">
            <InputFieldsSection
              onRoute={callback}
              tripQueryVariables={tripQueryVariables}
              setTripQueryVariables={setTripQueryVariables}
              loading={loading}
            ></InputFieldsSection>
          </div>
          <div className="box trip-section">
            <ItineraryListContainer
              tripQueryResult={tripQueryResult}
              selectedTripPatternIndex={selectedTripPatternIndex}
              setSelectedTripPatternIndex={setSelectedTripPatternIndex}
              tripQueryVariables={tripQueryVariables}
              setTripQueryVariables={setTripQueryVariables}
              pageResults={callback}
              loading={loading}
            />
          </div>
          <div className="box map-section">
            <MapView
              tripQueryResult={tripQueryResult}
              tripQueryVariables={tripQueryVariables}
              setTripQueryVariables={setTripQueryVariables}
              selectedTripPatternIndex={selectedTripPatternIndex}
              loading={loading}
            />
          </div>
        </div>
      </TimeZoneContext.Provider>
    </div>
  );
}
