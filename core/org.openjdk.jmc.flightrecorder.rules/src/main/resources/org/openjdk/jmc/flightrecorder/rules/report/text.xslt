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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

<xsl:output method="text" indent="yes"/>

<xsl:template match="/reportcollection">
	<xsl:text>Flight Recording report</xsl:text>
	<xsl:text>&#xa;&#xa;</xsl:text>
	<xsl:apply-templates select="report" />
</xsl:template>

<xsl:template match="report/file">
	<xsl:text>File: </xsl:text>
	<xsl:value-of select="."/>
	<xsl:text>&#xa;&#xa;</xsl:text>
</xsl:template>

<xsl:template match="rule">
	<xsl:apply-templates select="name" />
	<xsl:apply-templates select="severity" />
	<xsl:apply-templates select="score" />
	<xsl:apply-templates select="message" />
	<xsl:apply-templates select="detailedmessage" />
	<xsl:apply-templates select="itemset" />
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="rule/name">
	<xsl:text>Rule: </xsl:text>
	<xsl:value-of select="."/>
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="rule/severity">
	<xsl:text>Severity: </xsl:text>
	<xsl:value-of select="."/>
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="rule/score">
	<xsl:text>Score: </xsl:text>
	<xsl:value-of select="format-number(.,'0.#')"/>
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="rule/message">
	<xsl:text>Message: </xsl:text>
	<xsl:value-of select="."/>
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="rule/detailedmessage">
	<xsl:text>Detailed message: </xsl:text>
	<xsl:value-of select="."/>
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="itemset">
	<xsl:apply-templates select="name"/>
	<xsl:apply-templates select="fields"/>
	<xsl:apply-templates select="items"/>
</xsl:template>

<xsl:template match="itemset/name">
	<xsl:text>Result item set: </xsl:text>
	<xsl:value-of select="."/>
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="fields">
	<xsl:for-each select="field">
		<xsl:value-of select="name" />
		<xsl:choose>
			<xsl:when test="position()!=last()">
				<xsl:text>;</xsl:text>
			</xsl:when>
		</xsl:choose>
	</xsl:for-each>
	<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="field/name">
	<xsl:value-of select="."/>
</xsl:template>

<xsl:template match="items">
	<xsl:for-each select="item">
		<xsl:for-each select="value">
			<xsl:value-of select="." />
			<xsl:choose>
				<xsl:when test="position()!=last()">
					<xsl:text>;</xsl:text>
				</xsl:when>
			</xsl:choose>
		</xsl:for-each>
		<xsl:text>&#xa;</xsl:text>
	</xsl:for-each>
</xsl:template>

</xsl:stylesheet>