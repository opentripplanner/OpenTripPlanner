import { Mode } from '../gql/graphql.ts';

export const getColorForMode = function (mode: Mode) {
  if (mode === Mode.Foot) return '#444';
  if (mode === Mode.Bicycle) return '#5076D9';
  if (mode === Mode.Scooter) return '#253664';
  if (mode === Mode.Car) return '#444';
  if (mode === Mode.Rail) return '#86BF8B';
  if (mode === Mode.Coach) return '#25642A';
  if (mode === Mode.Metro) return '#D9B250';
  if (mode === Mode.Bus) return '#25642A';
  if (mode === Mode.Tram) return '#D9B250';
  if (mode === Mode.Trolleybus) return '#25642A';
  if (mode === Mode.Water) return '#81304C';
  if (mode === Mode.Air) return '#81304C';
  if (mode === Mode.Cableway) return '#81304C';
  if (mode === Mode.Funicular) return '#81304C';
  if (mode === Mode.Monorail) return '#81304C';
  if (mode === Mode.Taxi) return '#81304C';
  return '#aaa';
};
