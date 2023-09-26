import { TripPattern, TripQuery } from '../gql/graphql.ts';
import { Accordion } from 'react-bootstrap';
import { useMemo } from 'react';

function ItineraryHeaderContent({
  tripPattern,
  itineraryIndex,
  earliestStartTime,
  latestEndTime,
}: {
  tripPattern: TripPattern;
  itineraryIndex: number;
  earliestStartTime: string | null;
  latestEndTime: string | null;
}) {
  // var maxSpan = itin.tripPlan.latestEndTime - itin.tripPlan.earliestStartTime;
  const maxSpan = useMemo(
    () => new Date(latestEndTime!).getTime() - new Date(earliestStartTime!).getTime(),
    [earliestStartTime, latestEndTime],
  );

  // var startPct = (itin.itinData.startTime - itin.tripPlan.earliestStartTime) / maxSpan;
  const startPct = useMemo(
    () => (new Date(tripPattern.aimedStartTime).getTime() - new Date(earliestStartTime!).getTime()) / maxSpan,
    [tripPattern.aimedStartTime, earliestStartTime],
  );

  // var itinSpan = itin.getEndTime() - itin.getStartTime();
  const itinSpan = useMemo(
    () => new Date(tripPattern.aimedEndTime).getTime() - new Date(tripPattern.aimedStartTime).getTime(),
    [tripPattern.aimedStartTime, tripPattern.aimedEndTime],
  );

  // TODO this should be calculated from DOM
  const containerWidth = 300;

  // var timeWidth = 40;
  const timeWidth = 40;
  // var startPx = 20 + timeWidth,
  const startPx = 20 + timeWidth;
  //   endPx = div.width() - timeWidth - (itin.groupSize ? 48 : 0);
  const endPx = containerWidth || 300 - timeWidth;

  console.log({ maxSpan, startPct, itinSpan, endPx });

  // var pxSpan = endPx - startPx;
  const pxSpan = endPx - startPx;
  // var leftPx = startPx + startPct * pxSpan;
  const leftPx = startPx + startPct * pxSpan;
  // var widthPx = pxSpan * (itinSpan / maxSpan);
  const widthPx = pxSpan * (itinSpan / maxSpan);

  return (
    <div style={{ position: 'relative' }}>
      <div style={{ position: 'absolute' }}>{itineraryIndex + 1}.</div>
      <div
        style={{
          position: 'absolute',
          width: `${widthPx + 5}px`,
          height: '2px',
          left: `${leftPx - 2}px`,
          top: '9px',
          background: 'black',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: `${leftPx - timeWidth}px`,
          background: 'black',
          color: 'white',
        }}
      >
        {new Date(tripPattern.aimedStartTime).toLocaleTimeString('en-US', {
          timeStyle: 'short',
          hourCycle: 'h24',
        })}
      </div>

      <div
        style={{
          position: 'absolute',
          left: `${leftPx + widthPx + 2}px`,
          background: 'black',
          color: 'white',
        }}
      >
        {new Date(tripPattern.aimedEndTime).toLocaleTimeString('en-US', {
          timeStyle: 'short',
          hourCycle: 'h24',
        })}
      </div>
    </div>
  );
}

{
  /*<div*/
}
{
  /*  style={{*/
}
{
  /*    position: 'absolute',*/
}
{
  /*    width: '142.2935254296129px',*/
}
{
  /*    height: '2px',*/
}
{
  /*    left: '58px',*/
}
{
  /*    top: '9px',*/
}
{
  /*    background: 'black',*/
}
{
  /*  }}*/
}
{
  /*></div>*/
}
{
  /*<div className="otp-itinsAccord-header-time" style={{ left: '20px' }}>*/
}
{
  /*  {tripPattern.aimedStartTime}*/
}
{
  /*</div>*/
}
{
  /*<div className="otp-itinsAccord-header-time" style={{ left: '199.2935254296129px' }}>*/
}
{
  /*  {tripPattern.aimedEndTime}*/
}
{
  /*</div>*/
}
{
  /*<div*/
}
{
  /*  className="otp-itinsAccord-header-segment"*/
}
{
  /*  title="WALK"*/
}
{
  /*  style="width: 41.0712px; left: 61px; color: rgb(255, 255, 255); background: rgb(68, 68, 68);"*/
}
{
  /*>*/
}
{
  /*  <div*/
}
{
  /*    className="mode-icon"*/
}
{
  /*    style="background: #fff; mask-image: url(images/mode/walk.png); -webkit-mask-image: url(images/mode/walk.png)"*/
}
{
  /*  ></div>*/
}
{
  /*</div>*/
}
{
  /*<div*/
}
{
  /*  className="otp-itinsAccord-header-segment"*/
}
{
  /*  title="SUBWAY Ruter 3 Kolsås - Mortensrud"*/
}
{
  /*  style="width: 45.0753px; left: 103.071px; color: rgb(255, 255, 255); background: rgb(236, 112, 12);"*/
}
{
  /*>*/
}
{
  /*  <div*/
}
{
  /*    className="mode-icon"*/
}
{
  /*    style="background: #FFFFFF; mask-image: url(images/mode/subway.png); -webkit-mask-image: url(images/mode/subway.png)"*/
}
{
  /*  ></div>*/
}
{
  /*  <span>3</span>*/
}
{
  /*</div>*/
}
{
  /*<div*/
}
{
  /*  className="otp-itinsAccord-header-segment"*/
}
{
  /*  title="WALK"*/
}
{
  /*  style="width: 48.147px; left: 149.147px; color: rgb(255, 255, 255); background: rgb(68, 68, 68);"*/
}
{
  /*>*/
}
{
  /*  <div*/
}
{
  /*    className="mode-icon"*/
}
{
  /*    style="background: #fff; mask-image: url(images/mode/walk.png); -webkit-mask-image: url(images/mode/walk.png)"*/
}
{
  /*  ></div>*/
}
{
  /*</div>*/
}

export function ItineraryListContainer({
  tripQueryResult, //selectedTripPatternIndex,
  //setSelectedTripPatternIndex,
}: {
  tripQueryResult: TripQuery | null;
  //selectedTripPatternIndex: number;
  //setSelectedTripPatternIndex: (selectedTripPatternIndex: number) => void;
}) {
  const earliestStartTime = useMemo(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.aimedStartTime;
        } else {
          return new Date(current?.aimedStartTime) < new Date(acc) ? current.aimedStartTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  const latestEndTime = useMemo<string | null>(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.aimedEndTime;
        } else {
          return new Date(current?.aimedEndTime) > new Date(acc) ? current.aimedEndTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  console.log({ earliestStartTime, latestEndTime });

  return (
    <section className="itinerary-list-container">
      <Accordion defaultActiveKey="0">
        {tripQueryResult &&
          tripQueryResult.trip.tripPatterns.map((tripPattern, itineraryIndex) => (
            <Accordion.Item eventKey={`${itineraryIndex}`} key={`${itineraryIndex}`}>
              <Accordion.Header>
                <ItineraryHeaderContent
                  tripPattern={tripPattern}
                  itineraryIndex={itineraryIndex}
                  earliestStartTime={earliestStartTime}
                  latestEndTime={latestEndTime}
                />
              </Accordion.Header>
              <Accordion.Body>TODO</Accordion.Body>
            </Accordion.Item>
          ))}
      </Accordion>
    </section>
  );
}
