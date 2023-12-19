import { Mode } from '../gql/graphql.ts';

export function isTransitMode(mode: Mode) {
  return (
    mode === Mode.Rail ||
    mode === Mode.Coach ||
    mode === Mode.Metro ||
    mode === Mode.Bus ||
    mode === Mode.Tram ||
    mode === Mode.Water ||
    mode === Mode.Air ||
    mode === Mode.Cableway ||
    mode === Mode.Funicular ||
    mode === Mode.Trolleybus ||
    mode === Mode.Monorail ||
    mode === Mode.Taxi
  );
}
