import { Mode } from '../gql/graphql.ts';
import { Leg } from '../static/query/tripQueryTypes';

const getColorForMode = function (mode: Mode) {
  if (mode === Mode.Foot) return '#191616';
  if (mode === Mode.Bicycle) return '#5076D9';
  if (mode === Mode.Scooter) return '#14b7ca';
  if (mode === Mode.Car) return '#7e7e7e';
  if (mode === Mode.Rail) return '#86BF8B';
  if (mode === Mode.Coach) return '#25642A';
  if (mode === Mode.Metro) return '#D9B250';
  if (mode === Mode.Bus) return '#fe0000';
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

/**
 * Extract a line color from a leg. If there isn't one given by its line, this method returns a fallback color.
 */
export const getColorForLeg = function (leg: Leg) {
  if (leg.line?.presentation?.colour) {
    return `#${leg.line.presentation.colour}`;
  } else {
    return getColorForMode(leg.mode);
  }
};
