<?xml version="1.0" encoding="ISO-8859-1"?>
<!--   
   Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
   
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
   
   The contents of this file are subject to the terms of either the Universal Permissive License 
   v 1.0 as shown at http://oss.oracle.com/licenses/upl
   
   or the following license:
   
   Redistribution and use in source and binary forms, with or without modification, are permitted
   provided that the following conditions are met:
   
   1. Redistributions of source code must retain the above copyright notice, this list of conditions
   and the following disclaimer.
   
   2. Redistributions in binary form must reproduce the above copyright notice, this list of
   conditions and the following disclaimer in the documentation and/or other materials provided with
   the distribution.
   
   3. Neither the name of the copyright holder nor the names of its contributors may be used to
   endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
   FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
   CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
   WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output method="text" indent="yes"/>

<!-- Replace \ and then replace quotes. -->
  <xsl:template name="escape-everything">
    <xsl:param name="string"/>
    <xsl:choose>
    <!-- Start with backslash, then quotes and finally the control characters -->
      <xsl:when test="contains($string,'\')">
      	<xsl:variable name="before" select="substring-before($string,'\')"/>
      	<xsl:variable name="after" select="substring-after($string,'\')"/>
      	<xsl:call-template name="escape-quotes">           
          <xsl:with-param name="string" select="concat($before, '\\')"/>
        </xsl:call-template>
        <xsl:call-template name="escape-everything">
          <xsl:with-param name="string" select="$after"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="escape-quotes">
          <xsl:with-param name="string" select="$string"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

<!-- Replace quotes and then the rest -->
<xsl:template name="escape-quotes">
  <xsl:param name="string"/>
  <xsl:choose>
    <xsl:when test="contains($string, '&quot;')">
      <xsl:call-template name="cleanup-string">
          <xsl:with-param name="string" select="concat(substring-before($string,'&quot;'),'\&quot;')"/>
        </xsl:call-template>
      <xsl:call-template name="escape-quotes">
        <xsl:with-param name="string" select="substring-after($string,'&quot;')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="cleanup-string">
          <xsl:with-param name="string" select="$string"/>
        </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!--  Replace tab, carriage return and line feed. -->
  <xsl:template name="cleanup-string">
    <xsl:param name="string"/>
    <xsl:choose>
      <xsl:when test="contains($string,'&#x9;')">
        <xsl:call-template name="cleanup-string">
          <xsl:with-param name="string" select="concat(substring-before($string,'&#x9;'),'\t',substring-after($string,'&#x9;'))"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="contains($string,'&#xA;')">
        <xsl:call-template name="cleanup-string">
          <xsl:with-param name="string" select="concat(substring-before($string,'&#xA;'),'\n',substring-after($string,'&#xA;'))"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="contains($string,'&#xD;')">
        <xsl:call-template name="cleanup-string">
          <xsl:with-param name="string" select="concat(substring-before($string,'&#xD;'),'\r',substring-after($string,'&#xD;'))"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$string"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>




<!-- Make sure strings does not contain invalid backslash, quotes or control characters. -->
<xsl:template match="name|message|detailedmessage|error">
  <xsl:call-template name="escape-everything">
    <xsl:with-param name="string" select="."/>
  </xsl:call-template>
</xsl:template>



<xsl:template match="/reportcollection">
[<xsl:apply-templates select="report" />
]</xsl:template>

<xsl:template match="report">
  {
   <xsl:apply-templates select="file"/>
	<xsl:if test="count(rule)>0">
   "rules": [<xsl:apply-templates select="rule"/>
            ]</xsl:if>
	<xsl:if test="error">
   "error": "<xsl:apply-templates select="error"/>"</xsl:if>
  }<xsl:if test="following-sibling::*">,</xsl:if></xsl:template>

<xsl:template match="file">"file": "<xsl:value-of select="."/>",</xsl:template>

<xsl:template match="rule">
              {
               "id": "<xsl:apply-templates select="id" />",
               "name": "<xsl:apply-templates select="name" />", <xsl:choose><xsl:when test="count(error)>0"><xsl:text>&#xa;               </xsl:text>"error": "<xsl:apply-templates select="error" />"</xsl:when><xsl:otherwise>
               "severity": "<xsl:apply-templates select="severity" />",
               "score": <xsl:apply-templates select="score" />,
               "message": "<xsl:apply-templates select="message" />",
               "detailedMessage": "<xsl:apply-templates select="detailedmessage" />"<xsl:apply-templates select="itemset" />
             	</xsl:otherwise>
             </xsl:choose>
              }<xsl:if test="following-sibling::*">,</xsl:if></xsl:template>


<xsl:template match="itemset">
	<xsl:variable name="item-fields" select="fields/*"/>,
<xsl:variable name="number-of-fields" select="count($item-fields)"/>               "items": [<xsl:for-each select="items/item">
			<xsl:variable name="item-values" select="value"/>
                         {<xsl:for-each select="$item-fields">
				<xsl:variable name="field-index" select="position()"/>
                          "<xsl:value-of select="normalize-space(.)"/>": "<xsl:call-template name="escape-everything"><xsl:with-param name="string" select="$item-values[position()=$field-index]"/></xsl:call-template>"<xsl:if test="not($field-index = $number-of-fields)">,</xsl:if></xsl:for-each>
                         }<xsl:if test="not(position() = last())">,</xsl:if></xsl:for-each>
		<xsl:apply-templates select="item"/>
                        ]</xsl:template>

</xsl:stylesheet>