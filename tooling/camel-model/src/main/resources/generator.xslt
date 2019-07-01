<?xml version="1.0"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:xs="http://www.w3.org/2001/XMLSchema"
				xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				xmlns:ns="urn:mynamespace"
				xsi:schemaLocation="http://www.w3.org/1999/XSL/Transform https://www.w3.org/TR/xslt-30/schema-for-xslt30.xsd">
	<xsl:output indent="yes" method="text"/>
	<xsl:function name="ns:substring-after-last" as="xs:string" >
		<xsl:param name="value" as="xs:string?"/>
		<xsl:param name="separator" as="xs:string"/>
		<xsl:choose>
			<xsl:when test="contains($value, $separator)">
				<xsl:value-of select="ns:substring-after-last(substring-after($value, $separator), $separator)" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$value" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	<xsl:template match="/">
		<done/>
		<xsl:result-document method="text" href="generated/org/apache/camel/model/ProcessorDefinition.java">
		<xsl:text>
package org.apache.camel.model;
import java.util.HashMap;
import java.util.Map;
public class ProcessorDefinition {
    protected Map&lt;String, Object&gt; properties = new HashMap&lt;&gt;();
    protected &lt;T&gt; T setProperty(String name, Object value) {
        this.properties.put(name, value);
        return (T) this;
    }
}
		</xsl:text>
		</xsl:result-document>
		<xsl:result-document method="text" href="generated/org/apache/camel/model/ExpressionDefinition.java">
		<xsl:text>
package org.apache.camel.model;
public class ExpressionDefinition {
}
		</xsl:text>
		</xsl:result-document>
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="/model/endpoints/*[not(following-sibling::*/@javaType = current()/@javaType)]">
		<xsl:variable name="baseName" select="substring-before(ns:substring-after-last(@javaType, '.'), 'Component')"/>
		<xsl:result-document method="text" href="generated/org/apache/camel/model/endpoints/{$baseName}EndpointBuilderFactory.java" expand-text="true">
<xsl:text>
package org.apache.camel.model.endpoints;

public interface {$baseName}EndpointBuilderFactory {'{'}
</xsl:text>
<xsl:text>

	interface {$baseName}EndpointConsumerBuilder extends EndpointConsumerBuilder {'{'}
</xsl:text>
<xsl:if test="property[contains(@label,'advanced')"><xsl:text>
        default Advanced{$baseName}EndpointConsumerBuilder advanced() {'{'}
            return (Advanced{$baseName}EndpointConsumerBuilder) this;
        {'}'}
</xsl:text></xsl:if>
<xsl:text>
    {'}'}
</xsl:text>
<xsl:if test="property[contains(@label,'advanced')"><xsl:text>
    interface Advanced{$baseName}EndpointConsumerBuilder extends {'{'}
        default {$baseName}EndpointConsumerBuilder basic() {'{'}
            return ({$baseName}EndpointConsumerBuilder) this;
        {'}'}
    {'}'}
    interface Advanced{$baseName}EndpointBuilder
            extends Advanced{$baseName}EndpointConsumerBuilder, Advanced{$baseName}EndpointProducerBuilder {
        default {$baseName}EndpointBuilder basic() {
            return ({$baseName}EndpointBuilder) this;
        }
	}
</xsl:text></xsl:if>
{'}'}
</xsl:text>
		</xsl:result-document>
	</xsl:template>
	<xsl:template match="/model/processors/*">
		<xsl:variable name="name" select="@name"/>
		<xsl:variable name="className" select="concat(translate(substring($name,1,1),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'), substring($name,2),'Definition')"/>
		<xsl:result-document method="text" href="generated/org/apache/camel/model/processors/{$className}.java">
<xsl:text>
package org.apache.camel.model.processors;

</xsl:text>
<xsl:if test="property[starts-with(@type, 'list(')]"><xsl:text>import java.util.List;
</xsl:text></xsl:if>
<xsl:if test="property[contains(@type, 'model:expression')]">
<xsl:text>import org.apache.camel.model.ExpressionDefinition;
</xsl:text></xsl:if>
<xsl:text>import org.apache.camel.model.ProcessorDefinition;

public class </xsl:text><xsl:value-of select="$className"/><xsl:text> extends ProcessorDefinition {
</xsl:text>
		<xsl:for-each select="property">
	<xsl:variable name="type"><xsl:call-template name="getJavaType"><xsl:with-param name="type" select="@type"/></xsl:call-template></xsl:variable>
<xsl:text>    public </xsl:text><xsl:value-of select="$className"/><xsl:text> </xsl:text><xsl:value-of select="@name"/><xsl:text>(</xsl:text><xsl:value-of select="$type"/><xsl:text> </xsl:text><xsl:value-of select="@name"/><xsl:text>) {
        return setProperty("</xsl:text><xsl:value-of select="@name"/><xsl:text>", </xsl:text><xsl:value-of select="@name"/><xsl:text>);
    }
</xsl:text>
			<xsl:if test="@type != 'string' and count(following-sibling::property[@name = current()/@name])=0">
<xsl:text>    public </xsl:text><xsl:value-of select="$className"/><xsl:text> </xsl:text><xsl:value-of select="@name"/><xsl:text>(String </xsl:text><xsl:value-of select="@name"/><xsl:text>) {
        return setProperty("</xsl:text><xsl:value-of select="@name"/><xsl:text>", </xsl:text><xsl:value-of select="@name"/><xsl:text>);
    }
</xsl:text>
			</xsl:if>
		</xsl:for-each>
<xsl:text>
}
</xsl:text>
		</xsl:result-document>
	</xsl:template>
	<xsl:template name="getJavaType">
		<xsl:param name="type"/>
		<xsl:choose>
			<xsl:when test="starts-with($type, 'list(')">
				<xsl:variable name="p" select="substring-before(substring-after($type, 'list('), ')')"/>
				<xsl:variable name="t">
					<xsl:call-template name="getJavaType"><xsl:with-param name="type" select="$p"/></xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat('List&lt;', $t, '&gt;')"/>
			</xsl:when>
			<xsl:when test="starts-with($type, 'model:')">
				<xsl:variable name="p" select="substring-after($type, 'model:')"/>
				<xsl:variable name="t" select="concat(translate(substring($p,1,1), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), substring($p,2))"/>
				<xsl:value-of select="concat($t, 'Definition')"/>
			</xsl:when>
			<xsl:when test="starts-with($type, 'java:')">
				<xsl:variable name="p" select="substring-after($type, 'java:')"/>
				<xsl:value-of select="$p"/>
			</xsl:when>
			<xsl:when test="starts-with($type, 'enum:')">
				<xsl:variable name="p" select="substring-before(substring-after($type, 'enum:'), '(')"/>
				<xsl:value-of select="$p"/>
			</xsl:when>
			<xsl:when test="$type = 'string'">String</xsl:when>
			<xsl:otherwise><xsl:value-of select="$type"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
