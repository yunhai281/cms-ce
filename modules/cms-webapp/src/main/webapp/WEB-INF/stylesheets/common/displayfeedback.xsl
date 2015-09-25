<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" exclude-result-prefixes="#all"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

	<xsl:template name="displayfeedback">
		<xsl:param name="addbr" select="false()"/>
      <table class="feedback" id="feedbackTable">
        <xsl:if test="not(/node()/feedback/@code)">
          <xsl:attribute name="style">display: none</xsl:attribute>
        </xsl:if>
        <tr>
          <td>
            <div id="feedbackDiv" class="feedbackDiv">
              <xsl:call-template name="feedbackcodes">
                <xsl:with-param name="code" select="/node()/feedback/@code"/>
              </xsl:call-template>
            </div>
          </td>
        </tr>
      </table>
		<xsl:if test="$addbr and (/node()/feedback/@code)">
			<br/>
		</xsl:if>
	</xsl:template>

	<xsl:template name="feedbackcodes">
		<xsl:param name="code"/>

		<xsl:choose>
			<xsl:when test="$code = '0'">%feedbackContentNothingToSave%</xsl:when>
			<xsl:when test="$code = '1' or $code = '101'">%feedbackContentSaved%</xsl:when>
			<xsl:when test="$code = '2' or $code = '102'">%feedbackContentPublished%</xsl:when>
			<xsl:when test="$code = '4'">%feedbackContentRejected%</xsl:when>
			<xsl:when test="$code = '5' or $code = '105'">%feedbackContentNewVersion%</xsl:when>
			<xsl:when test="$code = '6'">%feedbackResourceSaved%</xsl:when>
			<xsl:when test="$code = '7'">%feedbackVersionDeleted%</xsl:when>
			<xsl:when test="$code = '9'">%feedbackContentAlreadyDeleted%</xsl:when>
			<xsl:when test="$code = '10'">%feedbackNoReadAccess%</xsl:when>
		</xsl:choose>

		<xsl:if test="$code &gt; 100">
			<br/><font color="red">%feedbackWarningNotLatest%</font>
		</xsl:if>

	</xsl:template>

</xsl:stylesheet>