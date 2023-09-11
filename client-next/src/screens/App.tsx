import { MapView } from '../components/MapView.tsx';
import { NavBarContainer } from '../components/NavBarContainer.tsx';
import { SearchBarContainer } from '../components/SearchBarContainer.tsx';

export function App() {
  return (
    <div className="app">
      <NavBarContainer />
      <SearchBarContainer />
      <MapView />
    </div>
  );
}
