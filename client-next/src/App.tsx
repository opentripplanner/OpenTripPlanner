import {MapContainer} from "./MapContainer.tsx";
import {NavBarContainer} from "./NavBarContainer.tsx";
import {TripQueryContainer} from "./TripQueryContainer.tsx";

export function App() {
  return (
    <div className="app">
      <NavBarContainer />
      <TripQueryContainer />
      <MapContainer />
    </div>
  );
}
