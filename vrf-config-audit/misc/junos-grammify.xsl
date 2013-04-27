<?xml version="1.0"?>
<!--
  Name       : junos-grammify.xsl
  Description: Partial translation of JUNOS XSD into an EBNF grammar for ANTLR
  Author     : ruanoj@github
-->
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
  xmlns:junos="http://xml.juniper.net/junos/10.4R8/junos">

  <xsl:variable name="model" select="'mx960'"/>

  <xsl:template match="/">
  grammar JUNOS;
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="xsd:element">
    <xsl:choose>
      <xsl:when test="@minOccurs=0">
        <xsl:choose>
          <xsl:when test="@maxOccurs">
          (* '<xsl:value-of select="@name"/>'
            <xsl:if test="@type"> TYPE[<xsl:value-of select="@type"/>] </xsl:if>
            <xsl:apply-templates/>)*
          </xsl:when>
          <xsl:otherwise>
          (? '<xsl:value-of select="@name"/>'
            <xsl:if test="@type"> TYPE[<xsl:value-of select="@type"/>] </xsl:if>
            <xsl:apply-templates/>)?
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="xsd:annotation/xsd:appinfo/flag='identifier'">
          (identifier: <xsl:value-of select="@name"/> - <xsl:value-of select="xsd:annotation/xsd:appinfo/match/pattern"/>)
            <xsl:apply-templates/>
          </xsl:when>
          <xsl:when test="xsd:annotation/xsd:appinfo/flag='oneliner'">
          {oneliner: <xsl:value-of select="@name"/><xsl:apply-templates/>}
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@name"/>
            <xsl:if test="@type">
              TYPE[<xsl:value-of select="@type"/>]
            </xsl:if>
            <xsl:apply-templates/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

<!--
  [complexType] 
-->
  <xsl:template match="xsd:complexType">
    <xsl:value-of select="@name"/>:
    <xsl:apply-templates/>
  </xsl:template>

<!--
  [simpleType]
-->
  <xsl:template match="xsd:simpleType">
    <xsl:choose>
      <xsl:when test="xsd:restriction">
        <xsl:value-of select="xsd:restriction/@base"/>
      </xsl:when>
      <xsl:when test="@base='xsd:string'">
        STRING
      </xsl:when>
      <xsl:when test="xsd:extension">
        <xsl:value-of select="xsd:extension/@base"/>
      </xsl:when>
      <xsl:otherwise>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates/>
  </xsl:template>

<!--
  [simpleContent] 
-->
  <xsl:template match="xsd:simpleContent">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="xsd:restriction">
    [restriction][base:<xsl:value-of select="@base"/>] (<xsl:for-each select="xsd:enumeration"><xsl:value-of select="@value"/>|</xsl:for-each>) /*
    <xsl:if test="xsd:minInclusive"> minInc=<xsl:value-of select="xsd:minInclusive/@value"/></xsl:if>
    <xsl:if test="xsd:maxInclusive"> maxInc=<xsl:value-of select="xsd:maxInclusive/@value"/></xsl:if>
    <xsl:if test="xsd:minLength"> minLen=<xsl:value-of select="xsd:minLength/@value"/></xsl:if>
    <xsl:if test="xsd:maxLength"> maxLen=<xsl:value-of select="xsd:maxLength/@value"/></xsl:if> */
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="xsd:minInclusive">
    <xsl:value-of select="@value"/>
  </xsl:template>

  <xsl:template match="xsd:extension">
    <xsl:choose>
      <xsl:when test="@base='xsd:string'"> STRING ';'</xsl:when>
      <xsl:otherwise><xsl:value-of select="@base"/> ';'</xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="xsd:choice">
    <xsl:choose>
      <xsl:when test="@minOccurs=0">
        <!-- min occurrences = 0 -->
        <xsl:choose>
          <xsl:when test="@maxOccurs">
          CHOICE ( <xsl:apply-templates/> )* /* CHOICE* */
          </xsl:when>
          <xsl:otherwise>
          CHOICE ( <xsl:apply-templates/> )? /* CHOICE? */
          </xsl:otherwise>
        </xsl:choose>
        <!-- min occurrences = 0 -->
      </xsl:when>
      <xsl:otherwise>
        CHOICE ( <xsl:apply-templates/> ) /* CHOICE */
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="xsd:sequence">
  SEQ( <xsl:apply-templates/>) /* SEQ */
  </xsl:template>

  <!-- Do not copy model names to output -->
  <xsl:template match="product"/>
  <xsl:template match="products"/>
  <!-- Diverse elements that don't produce output -->
  <xsl:template match="message"/>
  <xsl:template match="regex-match"/>
  <xsl:template match="regex-match-error"/>
  <xsl:template match="xsd:annotation"/>
  <xsl:template match="xsd:attribute"/>
  <xsl:template match="xsd:documentation"/>
  <xsl:template match="xsd:enumeration"/>
  <xsl:template match="xsd:import"/>
  <xsl:template match="match"/>
  <xsl:template match="xsd:minInclusive"/>
  <xsl:template match="xsd:maxInclusive"/>
  <xsl:template match="xsd:minLength"/>
  <xsl:template match="xsd:maxLength"/>
  <xsl:template match="flag">
    <xsl:choose>
      <xsl:when test="current()='autosort'"/>
      <xsl:when test="current()='current-product-support'"/>
      <xsl:when test="current()='hidden-from-cli'"/>
      <xsl:when test="current()='helpful'"/>
      <xsl:when test="current()='homogeneus'"/>
      <xsl:when test="current()='kilo'"/>
      <xsl:when test="current()='mandatory'"/>
      <xsl:when test="current()='mustquote'"/>
      <xsl:when test="current()='prune-lcc'"/>
      <xsl:when test="current()='ranged'"/>
      <xsl:when test="current()='remove-empty'"/>
      <xsl:when test="current()='root'"/>
      <xsl:when test="current()='traceflag'"/>
      <xsl:when test="current()='twig-dynamic-db-ok'"/>
      <xsl:otherwise>
        <xsl:copy>
        <xsl:copy-of select="@*"/>
        <xsl:apply-templates/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- All other tags, copy and process -->
  <xsl:template match="*">
    <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
<!-- vim: set ts=2 sw=2 nowrap ai si et: -->
