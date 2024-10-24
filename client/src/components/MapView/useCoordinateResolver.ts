import { Location } from '../../gql/graphql.ts';
import { useQuayCoordinateQuery } from '../../hooks/useQuayCoordinateQuery.ts';

interface Coordinates {
  latitude: number;
  longitude: number;
}

export function useCoordinateResolver(location: Location): Coordinates | undefined {
  const quay = useQuayCoordinateQuery(location);

  if (quay) {
    const { longitude, latitude } = quay;

    if (longitude && latitude) {
      return {
        longitude,
        latitude,
      };
    }
  }

  if (location.coordinates) {
    return {
      ...location.coordinates,
    };
  }

  return undefined;
}
