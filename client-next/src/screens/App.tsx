import {Stack} from "react-bootstrap";
import { MapView } from '../components/MapView.tsx';
import { NavBarContainer } from '../components/NavBarContainer.tsx';
import { SearchBarContainer } from '../components/SearchBarContainer.tsx';
import {ItineraryListContainer} from "../components/ItineraryListContainer.tsx";
import {DetailsViewContainer} from "../components/DetailsViewContainer.tsx";

export function App() {
  return (
    <div className="app">
      <NavBarContainer />
      <SearchBarContainer />
      <Stack direction="horizontal" gap={0}>
        <ItineraryListContainer />
        <MapView />
      </Stack>
      <DetailsViewContainer />
    </div>
  );
}
