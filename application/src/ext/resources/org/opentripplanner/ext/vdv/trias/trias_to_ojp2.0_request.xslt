<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:trias="http://www.vdv.de/trias"
                xmlns:siri="http://www.siri.org.uk/siri"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template name="triasConvertBooleanToString">
    <xsl:param name="input"/>
    <!-- Default value is false -->
    <xsl:choose>
      <xsl:when test="$input">
        <xsl:text>full</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>none</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="trias:Trias">
    <OJP xmlns="http://www.vdv.de/ojp" xmlns:siri="http://www.siri.org.uk/siri" version="2.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.vdv.de/ojp">
      <OJPRequest>
        <siri:ServiceRequest>
          <!-- Copy the RequestTimestamp from TIAS to OJP -->
          <xsl:copy-of select="//siri:RequestTimestamp"/>
          <!-- Copy the RequestorRef from TIAS to OJP -->
          <xsl:copy-of select="//siri:RequestorRef"/>
          <xsl:if test="//trias:StopEventRequest">
            <OJPStopEventRequest>
              <xsl:element name="siri:RequestTimestamp">
                <xsl:value-of select="//siri:RequestTimestamp"/>
              </xsl:element>
              <xsl:element name="siri:MessageIdentifier">
                <xsl:text>mymessage-1</xsl:text>
              </xsl:element>
              <Location>
                <PlaceRef>
                  <xsl:if test="//trias:StopPointRef">
                    <xsl:element name="siri:StopPointRef">
                      <xsl:value-of select="//trias:StopPointRef"/>
                    </xsl:element>
                  </xsl:if>
                  <xsl:if test="//trias:GeoPosition">
                    <GeoPosition>
                      <xsl:element name="siri:Latitude">
                        <xsl:value-of select="//trias:Latitude"/>
                      </xsl:element>
                      <xsl:element name="siri:Longitude">
                        <xsl:value-of select="//trias:Longitude"/>
                      </xsl:element>
                    </GeoPosition>
                  </xsl:if >
                </PlaceRef>
                <xsl:if test="//trias:DepArrTime">
                  <xsl:element name="DepArrTime">
                    <xsl:value-of select="//trias:DepArrTime"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IndividualTransportOptions">
                  <IndividualTransportOption>
                    <ItModeAndModeOfOperation>
                      <PersonalMode>foot</PersonalMode>
                    </ItModeAndModeOfOperation>
                    <MaxDistance>
                      <xsl:value-of select="//trias:MaxDistance"/>
                    </MaxDistance>
                  </IndividualTransportOption>
                </xsl:if>
              </Location>
              <Params>
                <xsl:if test="//trias:NumberOfResults">
                  <xsl:element name="NumberOfResults">
                    <xsl:value-of select="//trias:NumberOfResults"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:StopEventType">
                  <xsl:element name="StopEventType">
                    <xsl:value-of select="//trias:StopEventType"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludePreviousCalls">
                  <xsl:element name="IncludePreviousCalls">
                    <xsl:value-of select="//trias:IncludePreviousCalls"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeOnwardCalls">
                  <xsl:element name="IncludeOnwardCalls">
                    <xsl:value-of select="//trias:IncludeOnwardCalls"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeRealtimeData">
                  <xsl:choose>
                    <xsl:when test="//trias:IncludeRealtimeData='true'">
                      <xsl:element name="UseRealtimeData">
                        <xsl:text>full</xsl:text>
                      </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:element name="UseRealtimeData">
                        <xsl:text>none</xsl:text>
                      </xsl:element>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:if>
                <xsl:if test="//trias:TimeWindow">
                  <xsl:element name="TimeWindow">
                    <xsl:value-of select="//trias:TimeWindow"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:PtModeFilter">
                  <ModeFilter>
                    <xsl:for-each select="//trias:PtModeFilter//trias:PtMode">
                      <xsl:element name="PtMode">
                        <xsl:value-of select="."/>
                      </xsl:element>
                    </xsl:for-each>
                    <xsl:if test="//trias:Exclude">
                      <Exclude><xsl:value-of select="//trias:Exclude"/></Exclude>
                    </xsl:if>
                  </ModeFilter>
                </xsl:if>
                <xsl:if test="//trias:OperatorFilter">
                  <OperatorFilter>
                    <xsl:for-each select="//trias:OperatorFilter//trias:OperatorRef">
                      <xsl:element name="OperatorRef">
                        <xsl:value-of select="."/>
                      </xsl:element>
                    </xsl:for-each>
                    <xsl:if test="//trias:Exclude">
                      <Exclude><xsl:value-of select="//trias:Exclude"/></Exclude>
                    </xsl:if>
                  </OperatorFilter>
                </xsl:if>
                <xsl:if test="//trias:LineFilter">
                  <LineFilter>
                    <xsl:for-each select="//trias:LineFilter//trias:Line//trias:LineRef">
                      <Line>
                        <siri:LineRef>
                          <xsl:value-of select="."/>
                        </siri:LineRef>
                      </Line>
                    </xsl:for-each>
                    <xsl:if test="//trias:Exclude">
                      <Exclude><xsl:value-of select="//trias:Exclude"/></Exclude>
                    </xsl:if>
                  </LineFilter>
                </xsl:if>
              </Params>
            </OJPStopEventRequest>
          </xsl:if>
          <!-- TRIP INFO SERVICE -->
          <xsl:if test="//trias:TripInfoRequest">
            <OJPTripInfoRequest>
              <xsl:element name="siri:RequestTimestamp">
                <xsl:value-of select="//siri:RequestTimestamp"/>
              </xsl:element>
              <xsl:element name="siri:MessageIdentifier">
                <xsl:text>mymessage-1</xsl:text>
              </xsl:element>
              <xsl:if test="//trias:JourneyRef">
                <xsl:element name="JourneyRef">
                  <xsl:value-of select="//trias:JourneyRef"/>
                </xsl:element>
              </xsl:if>
              <xsl:if test="//trias:OperatingDayRef">
                <xsl:element name="OperatingDayRef">
                  <xsl:value-of select="//trias:JOperatingDayRef"/>
                </xsl:element>
              </xsl:if>
              <Params>
                <xsl:if test="//trias:UseTimetabledDataOnly">
                  <xsl:element name="UseTimetabledDataOnly">
                    <xsl:value-of select="//trias:UseTimetabledDataOnly"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeCalls">
                  <xsl:element name="IncludeCalls">
                    <xsl:value-of select="//trias:IncludeCalls"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludePosition">
                  <xsl:element name="IncludePosition">
                    <xsl:value-of select="//trias:IncludePosition"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeService">
                  <xsl:element name="IncludeService">
                    <xsl:value-of select="//trias:IncludeService"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeOnwardCalls">
                  <xsl:element name="IncludeOnwardCalls">
                    <xsl:value-of select="//trias:IncludeOnwardCalls"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeRealtimeData">
                  <xsl:element name="IncludeStopHierarchy">
                    <xsl:text>all</xsl:text>
                    <!-- to be discussed -->
                  </xsl:element>
                </xsl:if>
              </Params>
            </OJPTripInfoRequest>
          </xsl:if>
          <xsl:if test="//trias:LocationInformationRequest">
            <OJPLocationInformationRequest>
              <xsl:element name="siri:RequestTimestamp">
                <xsl:value-of select="//siri:RequestTimestamp"/>
              </xsl:element>
              <xsl:element name="siri:MessageIdentifier">
                <xsl:text>mymessage-1</xsl:text>
              </xsl:element>
              <xsl:choose>
                <xsl:when test="//trias:InitialInput">
                  <InitialInput>
                    <xsl:choose>
                      <xsl:when test="//trias:LocationName">
                        <xsl:element name="Name">
                          <xsl:value-of select="//trias:LocationName"/>
                        </xsl:element>
                      </xsl:when>
                      <xsl:when test="//trias:GeoPosition">
                        <GeoPosition>
                          <xsl:element name="siri:Longitude">
                            <xsl:value-of select="//trias:Longitude"/>
                          </xsl:element>
                          <xsl:element name="siri:Latitude">
                            <xsl:value-of select="//trias:Latitude"/>
                          </xsl:element>
                        </GeoPosition>
                      </xsl:when>
                      <xsl:when test="//trias:GeoRestriction">
                        <GeoRestriction>
                          <Circle>
                            <Center>
                              <xsl:element name="siri:Longitude">
                                <xsl:value-of select="//trias:Longitude"/>
                              </xsl:element>
                              <xsl:element name="siri:Latitude">
                                <xsl:value-of select="//trias:Latitude"/>
                              </xsl:element>
                            </Center>
                            <xsl:element name="Radius">
                              <xsl:value-of select="//trias:Radius"/>
                            </xsl:element>
                          </Circle>
                        </GeoRestriction>
                      </xsl:when>
                    </xsl:choose>
                  </InitialInput>
                </xsl:when>
                <xsl:otherwise>
                  <!-- Do nothing or add alternative logic if the element should not be included -->
                </xsl:otherwise>
              </xsl:choose>
              <Restrictions>
                <xsl:if test="//trias:Type">
                  <xsl:element name="Type">
                    <xsl:value-of select="//trias:Type"/>
                    <!-- TODO eventuall function needed -->
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:NumberOfResults">
                  <xsl:element name="NumberOfResults">
                    <xsl:value-of select="//trias:NumberOfResults"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludePtModes">
                  <xsl:element name="IncludePtModes">
                    <xsl:value-of select="//trias:IncludePtModes"/>
                  </xsl:element>
                </xsl:if>
              </Restrictions>
            </OJPLocationInformationRequest>
          </xsl:if>
          <!-- TRIP SERVICE -->
          <xsl:if test="//trias:TripRequest">
            <OJPTripRequest>
              <xsl:element name="siri:RequestTimestamp">
                <xsl:value-of select="//siri:RequestTimestamp"/>
              </xsl:element>
              <xsl:element name="siri:MessageIdentifier">
                <xsl:text>mymessage-1</xsl:text>
              </xsl:element>
              <Origin>
                <PlaceRef>
                  <xsl:choose>
                    <xsl:when test="//trias:Origin/trias:LocationRef/trias:StopPointRef">
                      <xsl:element name="siri:StopPointRef">
                        <xsl:value-of select="//trias:Origin/trias:LocationRef/trias:StopPointRef"/>
                      </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:message terminate="yes">Error: The condition is not met.</xsl:message>
                      <!-- also handling coordinates, StopPlace etc -->
                    </xsl:otherwise>
                  </xsl:choose>
                  <Name>
                    <xsl:element name="Text">
                      <xsl:text>Will be ignored</xsl:text>
                    </xsl:element>
                  </Name>
                </PlaceRef>
                <xsl:if test="//trias:DepArrTime">
                  <xsl:element name="DepArrTime">
                    <xsl:value-of select="//trias:DepArrTime"/>
                  </xsl:element>
                </xsl:if>
              </Origin>
              <Destination>
                <PlaceRef>
                  <xsl:choose>
                    <xsl:when test="//trias:Destination/trias:LocationRef/trias:StopPointRef">
                      <xsl:element name="siri:StopPointRef">
                        <xsl:value-of
                                select="//trias:Destination/trias:LocationRef/trias:StopPointRef"/>
                      </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:message terminate="yes">Error: The condition is not met.</xsl:message>
                      <!-- also handling coordinates, StopPlace etc -->
                    </xsl:otherwise>
                  </xsl:choose>
                  <Name>
                    <xsl:element name="Text">
                      <xsl:text>Will be ignored</xsl:text>
                    </xsl:element>
                  </Name>
                </PlaceRef>
                <xsl:if test="//trias:DepArrTime">
                  <xsl:element name="DepArrTime">
                    <xsl:value-of select="//trias:DepArrTime"/>
                  </xsl:element>
                </xsl:if>
              </Destination>
              <!-- Handling of Via -->
              <!-- Handling of NoChangeAt -->
              <Params>
                <!-- more parameters -->
                <xsl:if test="//trias:NumberOfResults">
                  <xsl:element name="NumberOfResults">
                    <xsl:value-of select="//trias:NumberOfResults"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeTrackSections">
                  <xsl:element name="IncludeTrackSections">
                    <xsl:value-of select="//trias:IncludeTrackSections"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeLegProjection">
                  <xsl:element name="IncludeLegProjection">
                    <xsl:value-of select="//trias:IncludeLegProjection"/>
                  </xsl:element>
                </xsl:if>
                <xsl:if test="//trias:IncludeIntermediateStops">
                  <xsl:element name="IncludeIntermediateStops">
                    <xsl:value-of select="//trias:IncludeIntermediateStops"/>
                  </xsl:element>
                </xsl:if>
              </Params>
            </OJPTripRequest>
          </xsl:if>
        </siri:ServiceRequest>
      </OJPRequest>
    </OJP>
  </xsl:template>
</xsl:stylesheet>