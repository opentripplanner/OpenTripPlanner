<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:siri="http://www.siri.org.uk/siri" xmlns:ojp="http://www.vdv.de/ojp" xmlns="http://www.vdv.de/trias" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="ojp:OJP">
    <Trias version="1.2" xmlns="http://www.vdv.de/trias" xmlns:siri="http://www.siri.org.uk/siri" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.vdv.de/trias  file:///C:/Users/ue71603/MG_Daten/github/TRIAS/Trias.xsd">
      <xsl:apply-templates select="ojp:OJPResponse"/>
    </Trias>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:OJPResponse">
    <xsl:apply-templates select="siri:ServiceDelivery"/>
  </xsl:template>
  <!--*********************************************-->

  <xsl:template match="siri:ServiceDelivery">
    <ServiceDelivery>
      <siri:ResponseTimestamp>
        <xsl:value-of select="//siri:ResponseTimestamp[1]"/>
      </siri:ResponseTimestamp>
      <siri:ProducerRef>
        <xsl:value-of select="//siri:ProducerRef[1]"/>
      </siri:ProducerRef>
      <Language>en</Language> <!-- TODO hack -->
      <xsl:apply-templates select="ojp:OJPStopEventDelivery"/>
      <xsl:apply-templates select="siri:ErrorCondition"/>

    </ServiceDelivery>
  </xsl:template>
  <!--*********************************************-->

  <xsl:template match="ojp:OJPStopEventDelivery">
    <DeliveryPayload>
      <StopEventResponse>
        <xsl:apply-templates select="ojp:StopEventResponseContext"/>
        <xsl:apply-templates select="ojp:StopEventResult"/>
      </StopEventResponse>
    </DeliveryPayload>
  </xsl:template>

  <!--*********************************************-->

  <xsl:template match="siri:ErrorCondition">
    <ErrorCondition>
      <xsl:value-of select="node()"/>
    </ErrorCondition>
  </xsl:template>


  <!--*********************************************-->
  <xsl:template match="ojp:StopEventResponseContext">
    <StopEventResponseContext>
      <xsl:apply-templates select="ojp:Places"/>
    </StopEventResponseContext>
  </xsl:template>
  <xsl:template match="ojp:Places">
    <Locations>
      <xsl:apply-templates select="ojp:Place"/>
    </Locations>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopEventResult">
    <StopEventResult>
      <ResultId>
        <xsl:value-of select="ojp:Id"/>
      </ResultId>
      <xsl:apply-templates select="ojp:StopEvent"/>
    </StopEventResult>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:Place">
    <Location>
      <xsl:apply-templates select="ojp:StopPlace"/>
      <xsl:apply-templates select="ojp:StopPoint"/>
      <LocationName>
        <Text><xsl:value-of select="ojp:Name/ojp:Text"/></Text>
      </LocationName>
      <xsl:apply-templates select="ojp:GeoPosition"/>
    </Location>
    <!--*********************************************-->
  </xsl:template>
  <xsl:template match="ojp:GeoPosition">
    <GeoPosition>
      <Longitude>
        <xsl:value-of select="siri:Longitude"/>
      </Longitude>
      <Latitude>
        <xsl:value-of select="siri:Latitude"/>
      </Latitude>
    </GeoPosition>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopPlace">
    <StopPlace>
      <StopPlaceRef>
        <xsl:value-of select="ojp:StopPlaceRef"/>
      </StopPlaceRef>
      <StopPlaceName>
        <Text><xsl:value-of select="normalize-space(ojp:StopPlaceName)"/></Text>
      </StopPlaceName>

      <PrivateCode>
        <System><xsl:value-of select="ojp:PrivateCode/ojp:System"/></System>
        <Value><xsl:value-of select="ojp:PrivateCode/ojp:Value"/></Value>
      </PrivateCode>
      <!--<xsl:if test="ojp:TopographicPlaceRef">
      <TopographicPlaceRef><xsl:value-of select="ojp:TopographicPlaceRef"/></TopographicPlaceRef></xsl:if>-->
    </StopPlace>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopPoint">
    <StopPoint>
      <StopPointRef>
        <xsl:value-of select="siri:StopPointRef"/>
      </StopPointRef>
      <StopPointName>
        <xsl:apply-templates select="ojp:StopPointName"/>
      </StopPointName>
      <PrivateCode>
        <System><xsl:value-of select="ojp:PrivateCode/ojp:System"/>
        </System>
        <Value><xsl:value-of select="ojp:PrivateCode/ojp:Value"/></Value>
      </PrivateCode>
      <!-- <xsl:if test="ojp:TopographicPlaceRef">
      <TopographicPlaceRef><xsl:value-of select="ojp:TopographicPlaceRef"/></TopographicPlaceRef></xsl:if> -->
    </StopPoint>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:CallAtStop">
    <CallAtStop>
      <StopPointRef>
        <xsl:value-of select="siri:StopPointRef"/>
      </StopPointRef>
      <StopPointName>
        <xsl:apply-templates select="ojp:StopPointName"/>
      </StopPointName>
      <PlannedBay>
        <xsl:apply-templates select="ojp:PlannedQuay"/>
      </PlannedBay>
      <xsl:if test="ojp:EstimatedQuay">
        <EstimatedBay>
          <Text><xsl:value-of select="ojp:EstimatedQuay/ojp:Text"/></Text>
        </EstimatedBay>
      </xsl:if>
      <xsl:apply-templates select="ojp:ServiceArrival"/>
      <xsl:apply-templates select="ojp:ServiceDeparture"/>
      <xsl:if test="ojp:Order">
        <StopSeqNumber><xsl:value-of select="ojp:Order"/></StopSeqNumber>
      </xsl:if>
      <xsl:if test="ojp:RequestStop">
        <DemandStop><xsl:value-of select="ojp:RequestStop"/></DemandStop>
      </xsl:if>
      <xsl:if test="ojp:UnplannedStop">
        <UnplannedStop><xsl:value-of select="ojp:UnplannedStop"/></UnplannedStop>
      </xsl:if>
      <xsl:if test="ojp:NotServicedStop">
        <NotServicedStop><xsl:value-of select="ojp:NotServicedStop"/></NotServicedStop>
      </xsl:if>
      <xsl:if test="ojp:NoBoardingAtStop">
        <NoBoardingAtStop><xsl:value-of select="ojp:NoBoardingAtStop"/></NoBoardingAtStop>
      </xsl:if>
      <xsl:if test="ojp:NoAlightingAtStop">
        <NoAlightingAtStop><xsl:value-of select="ojp:NoAlightingAtStop"/></NoAlightingAtStop>
      </xsl:if>
    </CallAtStop>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:ServiceArrival">
    <ServiceArrival>
      <xsl:if test="ojp:TimetabledTime">
        <TimetabledTime><xsl:value-of select="ojp:TimetabledTime"/></TimetabledTime>
      </xsl:if>
      <xsl:if test="ojp:EstimatedTime">
        <EstimatedTime><xsl:value-of select="ojp:EstimatedTime"/></EstimatedTime>
      </xsl:if>
    </ServiceArrival>

  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:ServiceDeparture">
    <ServiceDeparture>
      <xsl:if test="ojp:TimetabledTime">
        <TimetabledTime><xsl:value-of select="ojp:TimetabledTime"/></TimetabledTime>
      </xsl:if>
      <xsl:if test="ojp:EstimatedTime">
        <EstimatedTime><xsl:value-of select="ojp:EstimatedTime"/></EstimatedTime>
      </xsl:if>
    </ServiceDeparture>

  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopEvent">
    <StopEvent>
      <xsl:apply-templates select="ojp:PreviousCall"/>
      <xsl:apply-templates select="ojp:ThisCall"/>
      <xsl:apply-templates select="ojp:OnwardCall"/>
      <xsl:apply-templates select="ojp:Service"/>
      <Extension>
        <xsl:if test="ojp:Service/ojp:TrainNumber">
          <TrainNumber><xsl:value-of select="ojp:Service/ojp:TrainNumber"/></TrainNumber>
        </xsl:if>
        <xsl:if test="ojp:Service/ojp:ProductCategory">
          <ProductCategory><xsl:value-of select="ojp:Service/ojp:ProductCategory/ojp:ShortName/ojp:Text"/></ProductCategory>
        </xsl:if>
      </Extension>
    </StopEvent>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:ThisCall">
    <ThisCall>
      <xsl:apply-templates select="ojp:CallAtStop"/>
    </ThisCall>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:PreviousCall">
    <PreviousCall>
      <xsl:apply-templates select="ojp:CallAtStop"/>
    </PreviousCall>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:OnwardCall">
    <OnwardCall>
      <xsl:apply-templates select="ojp:CallAtStop"/>
    </OnwardCall>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:Text">
    <Text><xsl:value-of select="text()"/></Text>
    <Language><xsl:value-of select="@xml:lang"/></Language>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:Service">
    <Service>
      <OperatingDayRef><xsl:value-of select="ojp:OperatingDayRef"/></OperatingDayRef>
      <JourneyRef><xsl:value-of select="ojp:JourneyRef"/></JourneyRef>
      <ServiceSection>
        <LineRef><xsl:value-of select="siri:LineRef"/></LineRef>
        <DirectionRef><xsl:value-of select="siri:DirectionRef"/></DirectionRef>
        <Mode>
          <PtMode><xsl:value-of select="ojp:Mode/ojp:PtMode"/></PtMode>
          <!-- Handling of SubMode -->
        </Mode>
        <PublishedLineName>
          <Text><xsl:value-of select="normalize-space(ojp:PublishedServiceName)"/></Text>
        </PublishedLineName>
        <OperatorRef><xsl:value-of select="siri:OperatorRef"/></OperatorRef>
      </ServiceSection>
      <OriginStopPointRef>
        <xsl:value-of select="ojp:OriginStopPointRef"/>
      </OriginStopPointRef>
      <OriginText>
        <xsl:apply-templates select="ojp:OriginText"/>
      </OriginText>
      <DestinationStopPointRef>
        <xsl:value-of select="ojp:DestinationStopPointRef"/>
      </DestinationStopPointRef>
      <DestinationText>
        <xsl:apply-templates select="ojp:DestinationText"/>
      </DestinationText>
      <RouteDescription>
        <xsl:apply-templates select="ojp:RouteDescription"/>
      </RouteDescription>
    </Service>
  </xsl:template>
</xsl:stylesheet>