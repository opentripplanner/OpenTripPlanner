import { graphql } from '../../gql';
import { print } from 'graphql/index';

export const query = graphql(`
  query trip(
    $from: Location!
    $to: Location!
    $arriveBy: Boolean
    $dateTime: DateTime
    $numTripPatterns: Int
    $searchWindow: Int
    $modes: Modes
    $itineraryFiltersDebug: ItineraryFilterDebugProfile
    $wheelchairAccessible: Boolean
    $pageCursor: String
  ) {
    trip(
      from: $from
      to: $to
      arriveBy: $arriveBy
      dateTime: $dateTime
      numTripPatterns: $numTripPatterns
      searchWindow: $searchWindow
      modes: $modes
      itineraryFilters: { debug: $itineraryFiltersDebug }
      wheelchairAccessible: $wheelchairAccessible
      pageCursor: $pageCursor
    ) {
      previousPageCursor
      nextPageCursor
      tripPatterns {
        aimedStartTime
        aimedEndTime
        expectedEndTime
        expectedStartTime
        duration
        distance
        generalizedCost
        legs {
          id
          mode
          aimedStartTime
          aimedEndTime
          expectedEndTime
          expectedStartTime
          realtime
          distance
          duration
          generalizedCost
          fromPlace {
            name
            quay {
              id
            }
          }
          toPlace {
            name
            quay {
              id
            }
          }
          toEstimatedCall {
            destinationDisplay {
              frontText
            }
          }
          line {
            publicCode
            name
            id
            presentation {
              colour
            }
          }
          authority {
            name
            id
          }
          pointsOnLink {
            points
          }
          interchangeTo {
            staySeated
          }
          interchangeFrom {
            staySeated
          }
        }
        systemNotices {
          tag
        }
      }
    }
  }
`);

export const queryAsString = print(query);
