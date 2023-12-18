import { Mode } from '../gql/graphql.ts';

export const getColorForMode = function (mode: Mode) {
  if (mode === Mode.Foot) return '#444';
  if (mode === Mode.Bicycle) return '#44f';
  if (mode === Mode.Scooter) return '#88f';
  if (mode === Mode.Car) return '#444';
  if (mode === Mode.Rail) return '#b00';
  if (mode === Mode.Coach) return '#0f0';
  if (mode === Mode.Metro) return '#f00';
  if (mode === Mode.Bus) return '#0f0';
  if (mode === Mode.Tram) return '#f00';
  if (mode === Mode.Trolleybus) return '#0f0';
  if (mode === Mode.Water) return '#f0f';
  if (mode === Mode.Air) return '#f0f';
  if (mode === Mode.Cableway) return '#f0f';
  if (mode === Mode.Funicular) return '#f0f';
  if (mode === Mode.Monorail) return '#f0f';
  if (mode === Mode.Taxi) return '#f0f';
  return '#aaa';
};
