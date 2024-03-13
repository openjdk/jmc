<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<html>
			<head>
				<meta http-equiv="Content-Language" content="en-us" />
				<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
				<title>Mission Control 9.0 - New and Noteworthy</title>
			</head>
			<style type="text/css">
				p, table, td, th { font-family: verdana, arial, helvetica, geneva; font-size: 10pt}
				h2 { font-family: verdana, arial, helvetica, geneva; font-size: 16pt; font-weight: bold ; line-height: 14px}
				h3 { font-family: verdana, arial, helvetica, geneva; font-size: 12pt; font-weight: bold}
				h1 { font-family: verdana, arial, helvetica, geneva; font-size: 18pt; font-weight: bold}
				body { font-family: verdana, arial, helvetica, geneva; font-size: 9pt; margin-top: 5mm; margin-left: 3mm}
			</style>
			<body>
				<h2>Mission Control 9.0 - New and Noteworthy</h2>
				<table border="0" cellpadding="10" cellspacing="0" width="80%">
					<tr>
						<td colspan="2">
							<hr />
						</td>
					</tr>
					<xsl:for-each select="notes/component">
						<tr>
							<td colspan="2">
								<h2>
									<xsl:value-of select="name" />
								</h2>
								<hr />
							</td>
						</tr>
						<xsl:for-each select="note">
							<tr>
								<td width="30%" valign="top" align="left">
									<b>
										<xsl:value-of select="title" />
									</b>
								</td>
								<td width="70%" valign="top">
									<xsl:value-of select="description" />
									<p>
										<a>
											<xsl:attribute name="href">images/<xsl:value-of select="image" /></xsl:attribute>
											<img>
												<xsl:attribute name="src">smallimages/<xsl:value-of select="image" /></xsl:attribute>
												<xsl:attribute name="alt"><xsl:value-of select="title" /></xsl:attribute>
											</img>
										</a>
									</p>
								</td>
							</tr>
							<tr>
								<td colspan="2">
									<hr />
								</td>
							</tr>
						</xsl:for-each>
					</xsl:for-each>
					<tr>
						<td colspan="2">
							<h2>Bug Fixes</h2>
							<hr />
						</td>
					</tr>
					<xsl:for-each select="notes/bugfixes/bugfix">
						<tr>
							<td colspan="2">
								<p>
									<b>Area: </b><xsl:value-of select="area" /><br />
									<b>Issue: </b><a href="https://bugs.openjdk.java.net/browse/JMC-{bugid}"><xsl:value-of select="bugid" /></a><br />
									<b>Synopsis: </b><xsl:value-of select="synopsis" />
								</p>
								<p>
									<xsl:value-of select="description" />
								</p>
							</td>
						</tr>
					</xsl:for-each>
					<tr>
						<td colspan="2">
							<br />
							<hr />
							<h2>Known Issues</h2>
							<hr />
						</td>
					</tr>
					<xsl:for-each select="notes/knownissues/issue">
						<tr>
							<td colspan="2">
								<p>
									<b>Area: </b><xsl:value-of select="area" /><br />
									<b>Issue: </b><a href="https://bugs.openjdk.java.net/browse/JMC-{bugid}"><xsl:value-of select="bugid" /></a><br />
									<b>Synopsis: </b>
									<xsl:value-of select="synopsis" />
								</p>
								<p>
									<xsl:value-of select="description" />
								</p>
							</td>
						</tr>
					</xsl:for-each>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
