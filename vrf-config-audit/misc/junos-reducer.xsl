<?xml version="1.0"?>
<!--
  Name       : junos-reducer.xsl
  Description: Purge elements from JUNOS XMLSchema that are not supported by 
               a specified device model
  Author     : ruanoj@github

  This version worked on with JUNOS 11.4S xml schema

  Valid product strings (as extracted from XSD file):
  TXMatrix ex-xre ex2200-24p-4g ex2200-24t-4g ex2200-24t-dc-4g ex2200-48p-4g
  ex2200-48t-4g ex2200-c-12p-2g ex2200-c-12t-2g ex3200-24p ex3200-24t ex3200-48p
  ex3200-48t ex3300-24p ex3300-24t-dc ex3300-24t ex3300-48p ex3300-48t-bf
  ex3300-48t ex4200-24f ex4200-24p ex4200-24px ex4200-24t ex4200-48p ex4200-48px
  ex4200-48t ex4500-40f ex6210 ex8208 ex8216 fx-jvre fx1600_48 fx2160_48 ggsn-c
  j2300 j2320 j2350 j4300 j4350 j6300 j6350 jsr2300 jsr2320 jsr2350 jsr4300
  jsr4350 jsr6300 jsr6350 ln1000-v m10 m10i m120 m160 m20 m320 m40 m40e m5 m7i
  mag6610 mag6611 mag8600 mx10-t mx240 mx40-t mx480 mx5-t mx80-48t mx80-t mx80
  mx960 qfx3500 qfx3500s qfxc08-3008 sng-16s sng-8s srx100b srx100h srx110b-va
  srx110b-vb srx110b-wl srx110h-va-wl srx110h-va srx110h-vb-wl srx110h-vb
  srx110h-wl srx1400 srx210b srx210be srx210h-p-m srx210h-poe srx210h
  srx210he-p-m srx210he-poe srx210he srx220h-p-m srx220h-poe srx220h srx240b2
  srx240b srx240h-dc srx240h-p-m srx240h-poe srx240h2-dc srx240h2-poe srx240h2
  srx240h srx3400 srx3600 srx5600 srx5800 srx630 srx650 srx680 sumatra t320
  t640 txp
-->
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
  xmlns:junos="http://xml.juniper.net/junos/11.4R6/junos">

  <!-- VARIABLE: MODEL to look for -->
  <xsl:variable name="model" select="'mx960'"/>

  <xsl:template match="xsd:element">
    <xsl:choose>
      <!-- Required model is included in supported platforms: copy and process -->
      <xsl:when test="xsd:annotation/xsd:appinfo/products[product=$model]">
        <xsl:copy>
        <xsl:copy-of select="@*"/>
        <xsl:apply-templates/>
        </xsl:copy>
      </xsl:when>
      <!-- Required model not found: ignore -->
      <xsl:when test="xsd:annotation/xsd:appinfo/products">
        <!-- no output -->
      </xsl:when>
      <!-- Elements to ignore from output -->
      <xsl:when test="@ref='junos:comment'"/>
      <xsl:when test="@ref='undocumented'"/>
      <!-- Supported platforms not specified: copy and process -->
      <xsl:otherwise>
        <xsl:copy>
        <xsl:copy-of select="@*"/>
        <xsl:apply-templates/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Do not copy model names to output -->
  <xsl:template match="product"/>
  <xsl:template match="products"/>
  <xsl:template match="message"/>
  <xsl:template match="regex-match"/>
  <xsl:template match="regex-match-error"/>
  <xsl:template match="xsd:documentation"/>
  <xsl:template match="flag">
    <xsl:choose>
      <xsl:when test="current()='current-product-support'"/>
      <xsl:when test="current()='twig-dynamic-db-ok'"/>
      <xsl:otherwise> <!-- Other flags are copied as is -->
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
<!-- vim: set ts=2 sw=2 et: -->
