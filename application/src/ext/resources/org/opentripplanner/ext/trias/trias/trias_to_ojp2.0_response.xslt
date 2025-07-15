<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:trias="http://www.vdv.de/trias"
                xmlns:siri="http://www.siri.org.uk/siri"
                xmlns:ojp="http://www.vdv.de/ojp"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="ojp:OJP">
    <trias:Trias version="1.2"
           xmlns:trias="http://www.vdv.de/trias"
           xmlns:siri="http://www.siri.org.uk/siri"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.vdv.de/trias https://raw.githubusercontent.com/VDVde/TRIAS/refs/tags/v1.2/Trias.xsd">
      <xsl:apply-templates select="ojp:OJPResponse"/>
    </trias:Trias>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:OJPResponse">
    <xsl:apply-templates select="siri:ServiceDelivery"/>
  </xsl:template>
  <!--*********************************************-->

  <xsl:template match="siri:ServiceDelivery">
    <trias:ServiceDelivery>
      <siri:ResponseTimestamp>
        <xsl:value-of select="//siri:ResponseTimestamp[1]"/>
      </siri:ResponseTimestamp>
      <siri:ProducerRef>
        <xsl:value-of select="//siri:ProducerRef[1]"/>
      </siri:ProducerRef>
      <trias:Language>en</trias:Language> <!-- TODO hack -->
      <xsl:apply-templates select="ojp:OJPStopEventDelivery"/>
      <xsl:apply-templates select="siri:ErrorCondition"/>

    </trias:ServiceDelivery>
  </xsl:template>
  <!--*********************************************-->

  <xsl:template match="ojp:OJPStopEventDelivery">
    <trias:DeliveryPayload>
      <trias:StopEventResponse>
        <xsl:apply-templates select="ojp:StopEventResponseContext"/>
        <xsl:apply-templates select="ojp:StopEventResult"/>
      </trias:StopEventResponse>
    </trias:DeliveryPayload>
  </xsl:template>

  <!--*********************************************-->

  <xsl:template match="siri:ErrorCondition">
    <siri:ErrorCondition>
      <xsl:value-of select="node()"/>
    </siri:ErrorCondition>
  </xsl:template>


  <!--*********************************************-->
  <xsl:template match="ojp:StopEventResponseContext">
    <trias:StopEventResponseContext>
      <xsl:apply-templates select="ojp:Places"/>
    </trias:StopEventResponseContext>
  </xsl:template>
  <xsl:template match="ojp:Places">
    <trias:Locations>
      <xsl:apply-templates select="ojp:Place"/>
    </trias:Locations>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopEventResult">
    <trias:StopEventResult>
      <trias:ResultId>
        <xsl:value-of select="ojp:Id"/>
      </trias:ResultId>
      <xsl:apply-templates select="ojp:StopEvent"/>
    </trias:StopEventResult>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:Place">
    <trias:Location>
      <xsl:apply-templates select="ojp:StopPlace"/>
      <xsl:apply-templates select="ojp:StopPoint"/>
      <trias:LocationName>
        <trias:Text><xsl:value-of select="ojp:Name/ojp:Text"/></trias:Text>
      </trias:LocationName>
      <xsl:apply-templates select="ojp:GeoPosition"/>
    </trias:Location>
    <!--*********************************************-->
  </xsl:template>
  <xsl:template match="ojp:GeoPosition">
    <trias:GeoPosition>
      <trias:Longitude>
        <xsl:value-of select="siri:Longitude"/>
      </trias:Longitude>
      <trias:Latitude>
        <xsl:value-of select="siri:Latitude"/>
      </trias:Latitude>
    </trias:GeoPosition>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopPlace">
    <trias:StopPlace>
      <trias:StopPlaceRef>
        <xsl:value-of select="ojp:StopPlaceRef"/>
      </trias:StopPlaceRef>
      <trias:StopPlaceName>
        <trias:Text><xsl:value-of select="normalize-space(ojp:StopPlaceName)"/></trias:Text>
      </trias:StopPlaceName>

      <trias:PrivateCode>
        <trias:System><xsl:value-of select="ojp:PrivateCode/ojp:System"/></trias:System>
        <trias:Value><xsl:value-of select="ojp:PrivateCode/ojp:Value"/></trias:Value>
      </trias:PrivateCode>
      <!--<xsl:if test="ojp:TopographicPlaceRef">
      <TopographicPlaceRef><xsl:value-of select="ojp:TopographicPlaceRef"/></TopographicPlaceRef></xsl:if>-->
    </trias:StopPlace>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopPoint">
    <trias:StopPoint>
      <trias:StopPointRef>
        <xsl:value-of select="siri:StopPointRef"/>
      </trias:StopPointRef>
      <trias:StopPointName>
        <xsl:apply-templates select="ojp:StopPointName"/>
      </trias:StopPointName>
      <trias:PrivateCode>
        <trias:System><xsl:value-of select="ojp:PrivateCode/ojp:System"/>
        </trias:System>
        <Value><xsl:value-of select="ojp:PrivateCode/ojp:Value"/></Value>
      </trias:PrivateCode>
      <!-- <xsl:if test="ojp:TopographicPlaceRef">
      <TopographicPlaceRef><xsl:value-of select="ojp:TopographicPlaceRef"/></TopographicPlaceRef></xsl:if> -->
    </trias:StopPoint>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:CallAtStop">
    <trias:CallAtStop>
      <trias:StopPointRef>
        <xsl:value-of select="siri:StopPointRef"/>
      </trias:StopPointRef>
      <trias:StopPointName>
        <xsl:apply-templates select="ojp:StopPointName"/>
      </trias:StopPointName>
      <trias:PlannedBay>
        <xsl:apply-templates select="ojp:PlannedQuay"/>
      </trias:PlannedBay>
      <xsl:if test="ojp:EstimatedQuay">
        <trias:EstimatedBay>
          <Text><xsl:value-of select="ojp:EstimatedQuay/ojp:Text"/></Text>
        </trias:EstimatedBay>
      </xsl:if>
      <xsl:apply-templates select="ojp:ServiceArrival"/>
      <xsl:apply-templates select="ojp:ServiceDeparture"/>
      <xsl:if test="ojp:Order">
        <trias:StopSeqNumber><xsl:value-of select="ojp:Order"/></trias:StopSeqNumber>
      </xsl:if>
      <xsl:if test="ojp:RequestStop">
        <trias:DemandStop><xsl:value-of select="ojp:RequestStop"/></trias:DemandStop>
      </xsl:if>
      <xsl:if test="ojp:UnplannedStop">
        <trias:UnplannedStop><xsl:value-of select="ojp:UnplannedStop"/></trias:UnplannedStop>
      </xsl:if>
      <xsl:if test="ojp:NotServicedStop">
        <trias:NotServicedStop><xsl:value-of select="ojp:NotServicedStop"/></trias:NotServicedStop>
      </xsl:if>
      <xsl:if test="ojp:NoBoardingAtStop">
        <trias:NoBoardingAtStop><xsl:value-of select="ojp:NoBoardingAtStop"/></trias:NoBoardingAtStop>
      </xsl:if>
      <xsl:if test="ojp:NoAlightingAtStop">
        <trias:NoAlightingAtStop><xsl:value-of select="ojp:NoAlightingAtStop"/></trias:NoAlightingAtStop>
      </xsl:if>
    </trias:CallAtStop>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:ServiceArrival">
    <trias:ServiceArrival>
      <xsl:if test="ojp:TimetabledTime">
        <trias:TimetabledTime><xsl:value-of select="ojp:TimetabledTime"/></trias:TimetabledTime>
      </xsl:if>
      <xsl:if test="ojp:EstimatedTime">
        <trias:EstimatedTime><xsl:value-of select="ojp:EstimatedTime"/></trias:EstimatedTime>
      </xsl:if>
    </trias:ServiceArrival>

  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:ServiceDeparture">
    <trias:ServiceDeparture>
      <xsl:if test="ojp:TimetabledTime">
        <trias:TimetabledTime><xsl:value-of select="ojp:TimetabledTime"/></trias:TimetabledTime>
      </xsl:if>
      <xsl:if test="ojp:EstimatedTime">
        <trias:EstimatedTime><xsl:value-of select="ojp:EstimatedTime"/></trias:EstimatedTime>
      </xsl:if>
    </trias:ServiceDeparture>

  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:StopEvent">
    <trias:StopEvent>
      <xsl:apply-templates select="ojp:PreviousCall"/>
      <xsl:apply-templates select="ojp:ThisCall"/>
      <xsl:apply-templates select="ojp:OnwardCall"/>
      <xsl:apply-templates select="ojp:Service"/>
      <trias:Extension>
        <xsl:if test="ojp:Service/ojp:TrainNumber">
          <trias:TrainNumber><xsl:value-of select="ojp:Service/ojp:TrainNumber"/></trias:TrainNumber>
        </xsl:if>
        <xsl:if test="ojp:Service/ojp:ProductCategory">
          <trias:ProductCategory><xsl:value-of select="ojp:Service/ojp:ProductCategory/ojp:ShortName/ojp:Text"/></trias:ProductCategory>
        </xsl:if>
      </trias:Extension>
    </trias:StopEvent>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:ThisCall">
    <trias:ThisCall>
      <xsl:apply-templates select="ojp:CallAtStop"/>
      <xsl:if test="ojp:WalkDuration">
        <trias:WalkDuration><xsl:value-of select="ojp:WalkDuration"/></trias:WalkDuration>
      </xsl:if>
      <xsl:if test="ojp:WalkDistance">
        <trias:WalkDistance><xsl:value-of select="ojp:WalkDistance"/></trias:WalkDistance>
      </xsl:if>
    </trias:ThisCall>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:PreviousCall">
    <trias:PreviousCall>
      <xsl:apply-templates select="ojp:CallAtStop"/>
    </trias:PreviousCall>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:OnwardCall">
    <trias:OnwardCall>
      <xsl:apply-templates select="ojp:CallAtStop"/>
    </trias:OnwardCall>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:Text">
    <trias:Text><xsl:value-of select="text()"/></trias:Text>
    <trias:Language><xsl:value-of select="@xml:lang"/></trias:Language>
  </xsl:template>
  <!--*********************************************-->
  <xsl:template match="ojp:Service">
    <trias:Service>
      <trias:OperatingDayRef><xsl:value-of select="ojp:OperatingDayRef"/></trias:OperatingDayRef>
      <trias:JourneyRef><xsl:value-of select="ojp:JourneyRef"/></trias:JourneyRef>
      <trias:ServiceSection>
        <trias:LineRef><xsl:value-of select="siri:LineRef"/></trias:LineRef>
        <trias:DirectionRef><xsl:value-of select="siri:DirectionRef"/></trias:DirectionRef>
        <trias:Mode>
          <trias:PtMode><xsl:value-of select="ojp:Mode/ojp:PtMode"/></trias:PtMode>
          <!-- Handling of SubMode -->
        </trias:Mode>
        <trias:PublishedLineName>
          <trias:Text><xsl:value-of select="normalize-space(ojp:PublishedServiceName)"/></trias:Text>
        </trias:PublishedLineName>
        <trias:OperatorRef><xsl:value-of select="siri:OperatorRef"/></trias:OperatorRef>
      </trias:ServiceSection>
      <trias:OriginStopPointRef>
        <xsl:value-of select="ojp:OriginStopPointRef"/>
      </trias:OriginStopPointRef>
      <trias:OriginText>
        <xsl:apply-templates select="ojp:OriginText"/>
      </trias:OriginText>
      <trias:DestinationStopPointRef>
        <xsl:value-of select="ojp:DestinationStopPointRef"/>
      </trias:DestinationStopPointRef>
      <trias:DestinationText>
        <xsl:apply-templates select="ojp:DestinationText"/>
      </trias:DestinationText>
      <trias:RouteDescription>
        <xsl:apply-templates select="ojp:RouteDescription"/>
      </trias:RouteDescription>
      <xsl:if test="ojp:Cancelled">
        <trias:Cancelled>
          <xsl:apply-templates select="ojp:Cancelled"/>
        </trias:Cancelled>
      </xsl:if>
    </trias:Service>
  </xsl:template>
</xsl:stylesheet>