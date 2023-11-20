import { Stack } from 'react-bootstrap';
import { MapView } from '../components/MapView/MapView.tsx';
import { NavBarContainer } from '../components/NavBarContainer.tsx';
import { SearchBar } from '../components/SearchBar/SearchBar.tsx';
import { ItineraryListContainer } from '../components/ItineraryList/ItineraryListContainer.tsx';
import { DetailsViewContainer } from '../components/DetailsViewContainer.tsx';
import { useState } from 'react';
import { TripQueryVariables } from '../gql/graphql.ts';
import { useTripQuery } from '../hooks/useTripQuery.ts';

const INITIAL_VARIABLES: TripQueryVariables = {
  from: {},
  to: {},
  dateTime: new Date().toISOString(),
};

export function App() {
  const [tripQueryVariables, setTripQueryVariables] = useState<TripQueryVariables>(INITIAL_VARIABLES);
  const [tripQueryResult, callback] = useTripQuery(tripQueryVariables);
  const [selectedTripPatternIndex, setSelectedTripPatternIndex] = useState<number>(0);

  return (
    <div className="app">
      <NavBarContainer />
      <SearchBar
        onRoute={callback}
        tripQueryVariables={tripQueryVariables}
        setTripQueryVariables={setTripQueryVariables}
      />
      <Stack direction="horizontal" gap={0}>
        <ItineraryListContainer
          tripQueryResult={tripQueryResult}
          selectedTripPatternIndex={selectedTripPatternIndex}
          setSelectedTripPatternIndex={setSelectedTripPatternIndex}
          pageResults={callback}
        />
        <MapView
          tripQueryResult={tripQueryResult}
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
          selectedTripPatternIndex={selectedTripPatternIndex}
        />
      </Stack>
      <DetailsViewContainer />
    </div>
  );
}
