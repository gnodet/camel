<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dyn="http://exslt.org/dynamic" version="1.0" extension-element-prefixes="dyn">
	<xsl:output indent="yes" method="xml"/>
	<xsl:template match="/">
		<model>
			<xsl:comment>&#13;    - Languages&#13;    </xsl:comment>
			<languages>
				<xsl:for-each select="/model/languages/*">
					<xsl:element name="language">
						<xsl:attribute name="name"><xsl:value-of select="local-name(.)"/></xsl:attribute>
						<xsl:attribute name="display"><xsl:value-of select="language/title"/></xsl:attribute>
						<xsl:attribute name="label"><xsl:value-of select="language/label"/></xsl:attribute>
						<xsl:attribute name="extends">model:language</xsl:attribute>
						<xsl:attribute name="maven"><xsl:value-of select="concat(language/groupId, ':', language/artifactId, ':', language/version)"/></xsl:attribute>
						<xsl:attribute name="javaType"><xsl:value-of select="language/javaType"/></xsl:attribute>
						<xsl:if test="language/deprecated/text() = 'true'"><xsl:attribute name="deprecated">true</xsl:attribute></xsl:if>
						<xsl:if test="string-length(language/firstVersion/text())&gt;0"><xsl:attribute name="since"><xsl:value-of select="language/firstVersion"/></xsl:attribute></xsl:if>
						<xsl:attribute name="description"><xsl:value-of select="language/description"/></xsl:attribute>
						<xsl:for-each select="properties/*
									[local-name() != 'id' and local-name() != 'expression' and local-name() != 'trim']">
							<xsl:sort select="local-name(.)"/>
							<xsl:call-template name="property"/>
						</xsl:for-each>
					</xsl:element>
				</xsl:for-each>
			</languages>
			<xsl:comment>&#13;    - Dataformats&#13;    </xsl:comment>
			<dataFormats>
				<xsl:for-each select="/model/models/asn1 |
									  /model/models/avro |
									  /model/models/barcode |
									  /model/models/base64 |
									  /model/models/beanio |
									  /model/models/bindy |
									  /model/models/boon |
									  /model/models/cbor |
									  /model/models/crypto |
									  /model/models/csv |
									  /model/models/customDataFormat |
									  /model/models/fhirJson |
									  /model/models/fhirXml |
									  /model/models/flatpack |
									  /model/models/grok |
									  /model/models/gzipdeflater |
									  /model/models/hl7 |
									  /model/models/ical |
									  /model/models/jacksonxml |
									  /model/models/jaxb |
									  /model/models/json |
									  /model/models/jsonApi |
									  /model/models/lzf |
									  /model/models/mime-multipart |
									  /model/models/pgp |
									  /model/models/protobuf |
									  /model/models/rss |
									  /model/models/secureXML |
									  /model/models/soapjaxb |
									  /model/models/syslog |
									  /model/models/tarfile |
									  /model/models/thrift |
									  /model/models/tidyMarkup |
									  /model/models/univocity-csv |
									  /model/models/univocity-fixed |
									  /model/models/univocity-tsv |
									  /model/models/xmlrpc |
									  /model/models/xstream |
									  /model/models/yaml |
									  /model/models/zipdeflater |
									  /model/models/zipfile">
					<xsl:variable name="name" select="local-name()"/>
					<xsl:variable name="df" select="/model/dataformats/*[local-name() = $name]"/>
					<xsl:element name="dataFormat">
						<xsl:attribute name="name"><xsl:value-of select="local-name(.)"/></xsl:attribute>
						<xsl:attribute name="display"><xsl:value-of select="model/title"/></xsl:attribute>
						<xsl:attribute name="label"><xsl:value-of select="model/label"/></xsl:attribute>
						<xsl:attribute name="extends">model:dataFormat</xsl:attribute>
						<xsl:if test="$df">
							<xsl:attribute name="maven"><xsl:value-of select="concat($df/dataformat/groupId, ':', $df/dataformat/artifactId, ':', $df/dataformat/version)"/></xsl:attribute>
							<xsl:attribute name="javaType"><xsl:value-of select="$df/dataformat/modelJavaType"/></xsl:attribute>
						</xsl:if>
						<xsl:if test="model/deprecated/text() = 'true'"><xsl:attribute name="deprecated">true</xsl:attribute></xsl:if>
						<xsl:if test="string-length(model/firstVersion/text())&gt;0"><xsl:attribute name="since"><xsl:value-of select="model/firstVersion"/></xsl:attribute></xsl:if>
						<xsl:attribute name="description"><xsl:value-of select="model/description"/></xsl:attribute>
						<xsl:for-each select="properties/*
									[local-name() != 'id' and local-name() != 'contentTypeHeader']">
							<xsl:sort select="local-name(.)"/>
							<xsl:call-template name="property"/>
						</xsl:for-each>
					</xsl:element>
				</xsl:for-each>
			</dataFormats>
			<xsl:comment>&#13;    - LoadBalancers&#13;    </xsl:comment>
			<loadBalancers>
				<xsl:for-each select="/model/models/failover |
									  /model/models/random |
									  /model/models/customLoadBalancer |
									  /model/models/roundRobin |
									  /model/models/sticky |
									  /model/models/topic |
									  /model/models/weighted">
					<xsl:element name="loadBalancer">
						<xsl:attribute name="name"><xsl:value-of select="local-name(.)"/></xsl:attribute>
						<xsl:attribute name="display"><xsl:value-of select="model/title"/></xsl:attribute>
						<xsl:attribute name="label"><xsl:value-of select="model/label"/></xsl:attribute>
						<xsl:attribute name="extends">model:loadBalancer</xsl:attribute>
						<xsl:if test="model/deprecated/text() = 'true'"><xsl:attribute name="deprecated">true</xsl:attribute></xsl:if>
						<xsl:if test="string-length(model/firstVersion/text())&gt;0"><xsl:attribute name="since"><xsl:value-of select="model/firstVersion"/></xsl:attribute></xsl:if>
						<xsl:attribute name="description"><xsl:value-of select="model/description"/></xsl:attribute>
						<xsl:for-each select="properties/*[local-name() != 'id']">
							<xsl:sort select="local-name(.)"/>
							<xsl:call-template name="property"/>
						</xsl:for-each>
					</xsl:element>
				</xsl:for-each>
			</loadBalancers>
			<xsl:comment>&#13;    - Endpoints&#13;    </xsl:comment>
			<endpoints>
				<xsl:for-each select="/model/components/*">
					<xsl:variable name="parent">
						<xsl:choose>
							<xsl:when test="properties/backoffIdleThreshold">
								<xsl:value-of select="'scheduled'"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="'endpoint'"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:element name="endpoint">
						<xsl:attribute name="name"><xsl:value-of select="local-name(.)"/></xsl:attribute>
						<xsl:attribute name="display"><xsl:value-of select="component/title"/></xsl:attribute>
						<xsl:attribute name="label"><xsl:value-of select="component/label"/></xsl:attribute>
						<xsl:attribute name="extends"><xsl:value-of select="concat('model:', $parent)"/></xsl:attribute>
						<xsl:attribute name="javaType"><xsl:value-of select="component/javaType"/></xsl:attribute>
						<xsl:attribute name="maven"><xsl:value-of select="concat(component/groupId, ':', component/artifactId, ':', component/version)"/></xsl:attribute>
						<xsl:if test="component/deprecated/text() = 'true'"><xsl:attribute name="deprecated">true</xsl:attribute></xsl:if>
						<xsl:if test="component/consumerOnly/text() = 'true'"><xsl:attribute name="consumerOnly">true</xsl:attribute></xsl:if>
						<xsl:if test="component/producerOnly/text() = 'true'"><xsl:attribute name="producerOnly">true</xsl:attribute></xsl:if>
						<xsl:if test="component/async/text() = 'true'"><xsl:attribute name="async">true</xsl:attribute></xsl:if>
						<xsl:if test="component/lenientProperties/text() = 'true'"><xsl:attribute name="lenient">true</xsl:attribute></xsl:if>
						<xsl:if test="string-length(component/firstVersion/text())&gt;0"><xsl:attribute name="since"><xsl:value-of select="component/firstVersion"/></xsl:attribute></xsl:if>
						<xsl:attribute name="description"><xsl:value-of select="component/description"/></xsl:attribute>
						<xsl:for-each select="properties/*
									[local-name() != 'lazyStartProducer' and local-name() != 'bridgeErrorHandler' and local-name() != 'exceptionHandler'
									  and local-name() != 'exchangePattern' and local-name() != 'synchronous' and local-name() != 'basicPropertyBinding'
									  and ($parent != 'scheduled' or (local-name() != 'startScheduler'
											and local-name() != 'initialDelay' and local-name() != 'timeUnit'
											and local-name() != 'delay' and local-name() != 'useFixedDelay'
											and local-name() != 'pollStrategy' and local-name() != 'runLoggingLevel'
											and local-name() != 'sendEmptyMessageWhenIdle' and local-name() != 'greedy'
											and local-name() != 'scheduler' and local-name() != 'schedulerProperties'
											and local-name() != 'scheduledExecutorService' and local-name() != 'backoffMultiplier'
											and local-name() != 'backoffIdleThreshold' and local-name() != 'backoffErrorThreshold'))
									  ]">
							<xsl:sort select="local-name(.)"/>
							<xsl:call-template name="property"/>
						</xsl:for-each>
					</xsl:element>
				</xsl:for-each>
			</endpoints>
			<xsl:comment>&#13;    - Processors&#13;    </xsl:comment>
			<processors>
				<xsl:for-each select="/model/models">
					<xsl:for-each select="aggregate | bean | choice | claimCheck | convertBodyTo | delay | doCatch |
										  doFinally | doTry | dynamicRouter | enrich | filter | hystrix |
										  idempotentConsumer | inOnly | inOut | intercept | interceptFrom |
										  interceptSendToEndpoint | loadBalance | log | loop | marshal | multicast |
										  onCompletion | onException | onFallback | otherwise | pipeline | policy |
										  pollEnrich | process | recipientList | removeHeader | removeHeaders |
										  removeProperties | removeProperty | resequence | rollback | routingSlip |
										  saga | sample | script | setBody | setExchangePattern | setFaultBody |
										  setHeader | setProperty | sort | split | step | stop | threads | throttle |
										  throwException | ./to | toD | transacted | transform | unmarshal | validate |
										  when | whenSkipSendToEndpoint | wireTap">
						<xsl:element name="processor">
							<xsl:variable name="name" select="local-name()"/>
							<xsl:attribute name="name"><xsl:value-of select="$name"/></xsl:attribute>
							<xsl:attribute name="extends">model:processor</xsl:attribute>
							<xsl:attribute name="javaType"><xsl:value-of select="model/javaType"/></xsl:attribute>
							<xsl:attribute name="display"><xsl:value-of select="model/title"/></xsl:attribute>
							<xsl:attribute name="label"><xsl:value-of select="model/label"/></xsl:attribute>
							<xsl:if test="model/deprecated/text() = 'true'"><xsl:attribute name="deprecated">true</xsl:attribute></xsl:if>
							<xsl:if test="string-length(model/firstVersion/text())&gt;0"><xsl:attribute name="since"><xsl:value-of select="model/firstVersion"/></xsl:attribute></xsl:if>
							<xsl:attribute name="description"><xsl:value-of select="model/description"/></xsl:attribute>
							<xsl:for-each select="properties/*[($name != 'loadBalance' or local-name() != 'inheritErrorHandler') and local-name() != 'id' and local-name() != 'description']">
								<xsl:sort select="local-name(.)"/>
								<xsl:call-template name="property"/>
							</xsl:for-each>
						</xsl:element>
					</xsl:for-each>
				</xsl:for-each>
			</processors>
			<xsl:comment>&#13;    - Verbs&#13;    </xsl:comment>
			<verbs>
				<xsl:for-each select="/model/models">
					<xsl:for-each select="get | put | delete | post | head | patch">
						<xsl:element name="verb">
							<xsl:variable name="name" select="local-name()"/>
							<xsl:attribute name="name"><xsl:value-of select="$name"/></xsl:attribute>
							<xsl:attribute name="extends">model:verb</xsl:attribute>
							<xsl:attribute name="display"><xsl:value-of select="model/title"/></xsl:attribute>
							<xsl:attribute name="label"><xsl:value-of select="model/label"/></xsl:attribute>
							<xsl:if test="model/deprecated/text() = 'true'"><xsl:attribute name="deprecated">true</xsl:attribute></xsl:if>
							<xsl:if test="string-length(model/firstVersion/text())&gt;0"><xsl:attribute name="since"><xsl:value-of select="model/firstVersion"/></xsl:attribute></xsl:if>
							<xsl:attribute name="description"><xsl:value-of select="model/description"/></xsl:attribute>
							<xsl:for-each select="properties/*[local-name() != 'id' and
												               local-name() != 'description' and
												               local-name() != 'method' and
															   local-name() != 'uri' and
															   local-name() != 'consumes' and
															   local-name() != 'produces' and
															   local-name() != 'bindingMode' and
															   local-name() != 'skipBindingOnErrorCode' and
															   local-name() != 'clientRequestValidation' and
															   local-name() != 'enableCORS' and
															   local-name() != 'type' and
															   local-name() != 'outType' and
															   local-name() != 'toOrRoute' and
															   local-name() != 'routeId' and
															   local-name() != 'apiDocs']">
								<xsl:sort select="local-name(.)"/>
								<xsl:call-template name="property"/>
							</xsl:for-each>
						</xsl:element>
					</xsl:for-each>
				</xsl:for-each>
			</verbs>
			<xsl:comment>&#13;    - Definitions&#13;      </xsl:comment>
			<definitions>
				<xsl:for-each select="/model/models">
					<xsl:for-each select="*[local-name() != 'aggregate' and
											local-name() != 'bean' and
											local-name() != 'choice' and
											local-name() != 'claimCheck' and
											local-name() != 'convertBodyTo' and
											local-name() != 'delay' and
											local-name() != 'doCatch' and
											local-name() != 'doFinally' and
											local-name() != 'doTry' and
											local-name() != 'dynamicRouter' and
											local-name() != 'enrich' and
											local-name() != 'filter' and
											local-name() != 'hystrix' and
											local-name() != 'idempotentConsumer' and
											local-name() != 'inOnly' and
											local-name() != 'inOut' and
											local-name() != 'intercept' and
											local-name() != 'interceptFrom' and
											local-name() != 'interceptSendToEndpoint' and
											local-name() != 'loadBalance' and
											local-name() != 'log' and
											local-name() != 'loop' and
											local-name() != 'marshal' and
											local-name() != 'multicast' and
											local-name() != 'onCompletion' and
											local-name() != 'onException' and
											local-name() != 'onFallback' and
											local-name() != 'otherwise' and
											local-name() != 'pipeline' and
											local-name() != 'policy' and
											local-name() != 'pollEnrich' and
											local-name() != 'process' and
											local-name() != 'recipientList' and
											local-name() != 'removeHeader' and
											local-name() != 'removeHeaders' and
											local-name() != 'removeProperties' and
											local-name() != 'removeProperty' and
											local-name() != 'resequence' and
											local-name() != 'rollback' and
											local-name() != 'routingSlip' and
											local-name() != 'saga' and
											local-name() != 'sample' and
											local-name() != 'script' and
											local-name() != 'setBody' and
											local-name() != 'setExchangePattern' and
											local-name() != 'setFaultBody' and
											local-name() != 'setHeader' and
											local-name() != 'setProperty' and
											local-name() != 'sort' and
											local-name() != 'split' and
											local-name() != 'step' and
											local-name() != 'stop' and
											local-name() != 'threads' and
											local-name() != 'throttle' and
											local-name() != 'throwException' and
											local-name() != 'to' and
											local-name() != 'toD' and
											local-name() != 'transacted' and
											local-name() != 'transform' and
											local-name() != 'unmarshal' and
											local-name() != 'validate' and
											local-name() != 'when' and
											local-name() != 'whenSkipSendToEndpoint' and
											local-name() != 'wireTap' and
											local-name() != 'get' and
											local-name() != 'put' and
											local-name() != 'delete' and
											local-name() != 'post' and
											local-name() != 'head' and
											local-name() != 'patch' and
											local-name() != 'bean' and
											local-name() != 'constant' and
											local-name() != 'exchangeProperty' and
											local-name() != 'file' and
											local-name() != 'groovy' and
											local-name() != 'header' and
											local-name() != 'hl7terser' and
											local-name() != 'jsonpath' and
											local-name() != 'mvel' and
											local-name() != 'ognl' and
											local-name() != 'ref' and
											local-name() != 'simple' and
											local-name() != 'spel' and
											local-name() != 'tokenize' and
											local-name() != 'xpath' and
											local-name() != 'xquery' and
											local-name() != 'xtokenize' and
											local-name() != 'asn1' and
											local-name() != 'avro' and
											local-name() != 'barcode' and
											local-name() != 'base64' and
											local-name() != 'beanio' and
											local-name() != 'bindy' and
											local-name() != 'bindy-csv' and
											local-name() != 'bindy-fixed' and
											local-name() != 'bindy-kvp' and
											local-name() != 'boon' and
											local-name() != 'cbor' and
											local-name() != 'crypto' and
											local-name() != 'csv' and
											local-name() != 'fhirJson' and
											local-name() != 'fhirXml' and
											local-name() != 'flatpack' and
											local-name() != 'grok' and
											local-name() != 'gzip' and
											local-name() != 'gzipdeflater' and
											local-name() != 'hl7' and
											local-name() != 'ical' and
											local-name() != 'jacksonxml' and
											local-name() != 'jaxb' and
											local-name() != 'json' and
											local-name() != 'json-fastjson' and
											local-name() != 'json-gson' and
											local-name() != 'json-jackson' and
											local-name() != 'json-johnzon' and
											local-name() != 'json-xstream' and
											local-name() != 'jsonApi' and
											local-name() != 'lzf' and
											local-name() != 'mime-multipart' and
											local-name() != 'pgp' and
											local-name() != 'protobuf' and
											local-name() != 'rss' and
											local-name() != 'secureXML' and
											local-name() != 'soapjaxb' and
											local-name() != 'syslog' and
											local-name() != 'tarfile' and
											local-name() != 'thrift' and
											local-name() != 'tidyMarkup' and
											local-name() != 'univocity' and
											local-name() != 'univocity-csv' and
											local-name() != 'univocity-fixed' and
											local-name() != 'univocity-tsv' and
											local-name() != 'xmlrpc' and
											local-name() != 'xstream' and
											local-name() != 'yaml' and
											local-name() != 'yaml-snakeyaml' and
											local-name() != 'zip' and
											local-name() != 'zipdeflater' and
											local-name() != 'zipfile' and
											local-name() != 'customLoadBalancer' and
											local-name() != 'failover' and
											local-name() != 'random' and
											local-name() != 'roundRobin' and
											local-name() != 'sticky' and
											local-name() != 'topic' and
											local-name() != 'weighted'  ]">
						<xsl:element name="definition">
							<xsl:variable name="parent">
								<xsl:choose>
									<xsl:when test="properties/id/description/text() = 'Sets the id of this node'
												and properties/description/description/text() = 'Sets the description of this node'
												and properties/inheritErrorHandler/type/text() = 'boolean'">
										<xsl:value-of select="'processor'"/>
									</xsl:when>
									<xsl:when test="properties/id/description/text() = 'Sets the id of this node'
												and properties/description/description/text() = 'Sets the description of this node'">
										<xsl:value-of select="'node'"/>
									</xsl:when>
									<xsl:when test="properties/id/description = 'Sets the value of the id property.'">
										<xsl:value-of select="'identified'"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="''"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:attribute name="name"><xsl:value-of select="local-name(.)"/></xsl:attribute>
							<xsl:if test="$parent != ''"><xsl:attribute name="extends"><xsl:value-of select="concat('model:', $parent)"/></xsl:attribute></xsl:if>
							<xsl:attribute name="javaType"><xsl:value-of select="model/javaType"/></xsl:attribute>
							<xsl:attribute name="display"><xsl:value-of select="model/title"/></xsl:attribute>
							<xsl:attribute name="label"><xsl:value-of select="model/label"/></xsl:attribute>
							<xsl:if test="model/deprecated/text() = 'true'"><xsl:attribute name="deprecated">true</xsl:attribute></xsl:if>
							<xsl:if test="string-length(model/firstVersion/text())&gt;0"><xsl:attribute name="since"><xsl:value-of select="model/firstVersion"/></xsl:attribute></xsl:if>
							<xsl:attribute name="description"><xsl:value-of select="model/description"/></xsl:attribute>
							<xsl:for-each select="properties/*[
								($parent = 'processor' and local-name() != 'id' and local-name() != 'description' and local-name() != 'inheritErrorHandler') or
								($parent = 'node' and local-name() != 'id' and local-name() != 'description') or
								($parent = 'identified' and local-name() != 'id') or
								($parent != 'processor' and $parent != 'node' and $parent != 'identified')
							]">

								<xsl:sort select="local-name(.)"/>
								<xsl:call-template name="property"/>
							</xsl:for-each>
						</xsl:element>
					</xsl:for-each>
				</xsl:for-each>
			</definitions>
		</model>
	</xsl:template>

	<xsl:template name="property">
		<xsl:element name="property">
			<xsl:attribute name="name">
				<xsl:value-of select="local-name(.)"/>
			</xsl:attribute>
			<xsl:attribute name="type">
				<xsl:call-template name="getType">
					<xsl:with-param name="javaType" select="javaType"/>
					<xsl:with-param name="enums" select="enum"/>
				</xsl:call-template>
			</xsl:attribute>
			<xsl:attribute name="display">
				<xsl:value-of select="displayName"/>
			</xsl:attribute>
			<xsl:if test="string-length(label)&gt;0">
				<xsl:attribute name="label">
					<xsl:value-of select="label"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="required/text() = 'true'">
				<xsl:attribute name="required">true</xsl:attribute>
			</xsl:if>
			<xsl:if test="kind/text() != 'parameter' and kind/text() != 'attribute'">
				<xsl:attribute name="kind">
					<xsl:value-of select="kind"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="deprecated/text() = 'true'">
				<xsl:attribute name="deprecated">true</xsl:attribute>
			</xsl:if>
			<xsl:if test="secret/text() = 'true'">
				<xsl:attribute name="secret">true</xsl:attribute>
			</xsl:if>
			<xsl:if test="default">
				<xsl:attribute name="default">
					<xsl:value-of select="default"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:attribute name="description"><xsl:value-of select="description"/></xsl:attribute>
		</xsl:element>
	</xsl:template>
	<xsl:template name="getType">
		<xsl:param name="javaType" />
		<xsl:param name="enums" />
		<xsl:choose>
			<xsl:when test="'[]' = substring($javaType, string-length($javaType) - string-length('[]') + 1)">
				<xsl:variable name="t1">
					<xsl:call-template name="getType">
						<xsl:with-param name="javaType" select="substring-before($javaType, '[]')"/>
						<xsl:with-param name="enums" select="$enums"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat($t1, '[]')"/>
			</xsl:when>
			<xsl:when test="starts-with($javaType, 'java.util.List&lt;') and '&gt;' = substring($javaType, string-length($javaType) - string-length('&gt;') + 1)">
				<xsl:variable name="end" select="substring-after($javaType, '&lt;')"/>
				<xsl:variable name="params" select="substring($end, 0, string-length($end))"/>
				<xsl:variable name="t1">
					<xsl:call-template name="getType">
						<xsl:with-param name="javaType" select="$params"/>
						<xsl:with-param name="enums" select="$enums"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat('list(', $t1, ')')"/>
			</xsl:when>
			<xsl:when test="starts-with($javaType, 'java.util.Set&lt;') and '&gt;' = substring($javaType, string-length($javaType) - string-length('&gt;') + 1)">
				<xsl:variable name="end" select="substring-after($javaType, '&lt;')"/>
				<xsl:variable name="params" select="substring($end, 0, string-length($end))"/>
				<xsl:variable name="t1">
					<xsl:call-template name="getType">
						<xsl:with-param name="javaType" select="$params"/>
						<xsl:with-param name="enums" select="$enums"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat('set(', $t1, ')')"/>
			</xsl:when>
			<xsl:when test="starts-with($javaType, 'java.util.Map&lt;') and '&gt;' = substring($javaType, string-length($javaType) - string-length('&gt;') + 1)">
				<xsl:variable name="end" select="substring-after($javaType, '&lt;')"/>
				<xsl:variable name="params" select="substring($end, 0, string-length($end))"/>
				<xsl:variable name="t1">
					<xsl:call-template name="getType">
						<xsl:with-param name="javaType" select="substring-before($params, ',')"/>
						<xsl:with-param name="enums" select="$enums"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:variable name="t2">
					<xsl:call-template name="getType">
						<xsl:with-param name="javaType" select="substring-after($params, ',')"/>
						<xsl:with-param name="enums" select="$enums"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat('map(', $t1, ',', $t2, ')')"/>
			</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Object'">object</xsl:when>
			<xsl:when test="$javaType = 'java.lang.String'">string</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Byte' or $javaType = 'byte'">byte</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Integer' or $javaType = 'int'">int</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Long' or $javaType = 'long'">long</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Double' or $javaType = 'double'">double</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Character' or $javaType = 'char'">char</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Boolean' or $javaType = 'boolean'">boolean</xsl:when>
			<xsl:when test="$javaType = 'java.lang.Class&lt;?&gt;'">class</xsl:when>
			<xsl:when test="/model/models/*[model/javaType/text() = $javaType]">
				<xsl:value-of select="concat('model:', name(/model/models/*[model/javaType/text() = $javaType]))"/>
			</xsl:when>
			<xsl:when test="$javaType = 'org.apache.camel.model.ProcessorDefinition&lt;?&gt;'">model:processor</xsl:when>
			<xsl:when test="$javaType = 'org.apache.camel.model.language.NamespaceAwareExpression'">model:expression</xsl:when>
			<xsl:when test="$javaType = 'org.apache.camel.model.DataFormatDefinition'">model:dataFormat</xsl:when>
			<xsl:when test="$javaType = 'org.apache.camel.model.LoadBalancerDefinition'">model:loadBalancer</xsl:when>
			<!--
			<xsl:when test="$javaType = 'org.apache.camel.model.DataFormatDefinition'">model:dataformat</xsl:when>
			<xsl:when test="starts-with($javaType, 'org.apache.camel.model.') and 'Definition' = substring($javaType, string-length($javaType) - string-length('Definition') + 1)">
				<xsl:variable name="t">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="string1" select="$javaType"/>
						<xsl:with-param name="string2" select="'.'"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:variable name="t2">
					<xsl:value-of select="substring-before(substring($javaType, string-length($t) + 2), 'Definition')"/>
				</xsl:variable>
				<xsl:value-of select="concat('model:', translate(substring($t2, 1, 1), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), substring($t2, 2))"/>
			</xsl:when>
			<xsl:when test="$javaType = 'org.apache.camel.model.language.ExpressionDefinition'">model:expression</xsl:when>
			-->
			<xsl:when test="count($enums)&gt;0">
				<xsl:variable name="enums-str">
					<xsl:call-template name="join">
						<xsl:with-param name="valueList" select="$enums"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:variable name="raw">
					<xsl:choose>
						<xsl:when test="starts-with($javaType, 'org.apache.camel.component.')">
							<xsl:variable name="pkg">
								<xsl:call-template name="substring-before-last">
									<xsl:with-param name="string1" select="$javaType"/>
									<xsl:with-param name="string2" select="'.'"/>
								</xsl:call-template>
							</xsl:variable>
							<xsl:value-of select="substring($javaType, string-length($pkg) + 2)"/>
						</xsl:when>
						<xsl:when test="starts-with($javaType, 'org.apache.camel.model.dataformat.')">
							<xsl:variable name="pkg">
								<xsl:call-template name="substring-before-last">
									<xsl:with-param name="string1" select="$javaType"/>
									<xsl:with-param name="string2" select="'.'"/>
								</xsl:call-template>
							</xsl:variable>
							<xsl:value-of select="substring($javaType, string-length($pkg) + 2)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$javaType"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:value-of select="concat('enum:', $raw, '(', $enums-str, ')')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat('java:', $javaType)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="join">
		<xsl:param name="valueList" select="''"/>
		<xsl:param name="separator" select="','"/>
		<xsl:for-each select="$valueList">
			<xsl:choose>
				<xsl:when test="position() = 1">
					<xsl:value-of select="."/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="concat($separator, .) "/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
	<xsl:template name="substring-before-last">
		<xsl:param name="string1" select="''" />
		<xsl:param name="string2" select="''" />
		<xsl:if test="$string1 != '' and $string2 != ''">
			<xsl:variable name="head" select="substring-before($string1, $string2)" />
			<xsl:variable name="tail" select="substring-after($string1, $string2)" />
			<xsl:value-of select="$head" />
			<xsl:if test="contains($tail, $string2)">
				<xsl:value-of select="$string2" />
				<xsl:call-template name="substring-before-last">
					<xsl:with-param name="string1" select="$tail" />
					<xsl:with-param name="string2" select="$string2" />
				</xsl:call-template>
			</xsl:if>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
