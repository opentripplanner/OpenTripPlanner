import {MapContainer} from "./MapContainer.tsx";
import {NavBarContainer} from "./NavBarContainer.tsx";
import {JourneyPlannerRequestContainer} from "./JourneyPlannerRequestContainer.tsx";

export function App() {
  return (
    <div className="app">
      <NavBarContainer />
      <JourneyPlannerRequestContainer />
      <MapContainer />
    </div>
  );
}
