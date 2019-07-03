<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dyn="http://exslt.org/dynamic" version="1.0" extension-element-prefixes="dyn">
	<xsl:output indent="yes" method="xml"/>
	<xsl:template match="/">
		<model>
			<definitions>
				<definition name="model">
					<!--
					<property name="tracing" type="boolean"/>
					<property name="debugging" type="boolean"/>
					<property name="streamCaching" type="boolean"/>
					<property name="messageHistory" type="boolean"/>
					<property name="logMask" type="boolean"/>
					<property name="logExhaustedMessageBody" type="boolean"/>
					<property name="handleFault" type="boolean"/>
					<property name="delayer" type="long"/>
					<property name="autoStartup" type="boolean"/>
					<property name="shutdownRoute" type="enum">
						<java>org.apache.camel.ShutdownRoute</java>
						<enum>Default,Defer</enum>
					</property>
					<property name="shutdownRunningTask" display="Shutdown Running Task" type="enum">
						<java>org.apache.camel.ShutdownRunningTask</java>
						<enum>CompleteCurrentTaskOnly,CompleteAllTasks</enum>
						<description>To control how to shutdown the route.</description>
					</property>
					<property name="useMDCLogging" type="boolean"/>
					-->
					<property name="routes" type="list(model:route)"/>
				</definition>
				<definition name="identified" abstract="true">
					<property name="id" type="string" display="Id" description="Sets the value of the id property."/>
				</definition>
				<definition name="node" display="Node" abstract="true">
					<property name="id" type="string" display="Id" description="Sets the id of this node"/>
					<property name="description" type="string" display="Description" description="Sets the description of this node"/>
				</definition>
				<definition name="route" display="Route" label="configuration" extends="model:node">
					<property name="autoStartup" type="boolean" display="Auto Startup" description="Whether to auto start this route"/>
					<property name="delayer" type="long" display="Delayer" description="Whether to slow down processing messages by a given delay in msec."/>
					<property name="errorHandlerFactory" display="Error Handler Factory" type="java:org.apache.camel.ErrorHandlerFactory" description="Sets the error handler factory to use on this route"/>
					<property name="group" type="string" display="Group" description="The group that this route belongs to; could be the name of the RouteBuilder class or be explicitly configured in the XML. May be null."/>
					<property name="handleFault" type="boolean" display="Handle Fault" description="Whether handle fault is enabled on this route."/>
					<property name="input" type="model:from" display="Input" required="true" kind="element" description="Input to the route."/>
					<property name="inputType" type="model:dataType" display="Input Type"/>
					<property name="logMask" type="boolean" display="Log Mask" description="Whether security mask for Logging is enabled on this route."/>
					<property name="messageHistory" type="boolean" display="Message History" description="Whether message history is enabled on this route."/>
					<property name="outputs" type="list(model:processor)" display="Outputs" required="true" description="Outputs are processors that determines how messages are processed by this route."/>
					<property name="outputType" type="model:dataType" display="Output Type"/>
					<property name="routePolicies" type="list(java:org.apache.camel.spi.RoutePolicy)"  display="Route Policy" description="Reference to custom org.apache.camel.spi.RoutePolicy to use by the route. Multiple policies can be configured by separating values using comma."/>
					<property name="startupOrder" type="int" display="Startup Order" description="To configure the ordering of the routes being started"/>
					<property name="streamCache" type="boolean" display="Stream Cache" description="Whether stream caching is enabled on this route."/>
					<property name="shutdownRoute" type="enum:org.apache.camel.ShutdownRoute(Default,Defer)" display="Shutdown Route" description="To control how to shutdown the route."/>
					<property name="shutdownRunningTask" type="enum:org.apache.camel.ShutdownRunningTask(CompleteCurrentTaskOnly,CompleteAllTasks)" display="Shutdown Running Task" description="To control how to shutdown the route."/>
					<property name="trace" type="boolean" display="Trace" description="Whether tracing is enabled on this route."/>
				</definition>
				<definition name="from" display="From" label="eip,endpoint,routing" extends="model:node">
					<property name="endpoint" type="model:endpoint" display="Endpoint" required="true" description="Sets the endpoint to use"/>
				</definition>
				<definition name="dataType">
					<property name="urn" type="string"/>
					<property name="validate" type="boolean"/>
				</definition>


				<!--
				   - Processors
				   -->
				<xsl:comment>&#13;      - Processors&#13;      </xsl:comment>
				<definition name="processor" display="Processor" abstract="true" extends="model:node"/>

				<!--
				  - DataFormats
				  -->
				<xsl:comment>&#13;      - Endpoints&#13;      </xsl:comment>
				<definition name="dataFormat" abstract="true" extends="model:identified">
					<property name="contentTypeHeader" type="boolean" display="Content Type Header" description="Whether the data format should set the Content-Type header with the type from the data format if the data format is capable of doing so. For example application/xml for data formats marshalling to XML, or application/json for data formats marshalling to JSon etc."/>
				</definition>

				<!--
				   - Endpoints
				   -->
				<xsl:comment>&#13;      - Endpoints&#13;      </xsl:comment>
				<definition name="endpoint" abstract="true">
					<property name="basicPropertyBinding" type="boolean" display="Basic Property Binding" label="advanced" description="Whether the endpoint should use basic property binding (Camel 2.x) or the newer property binding with additional capabilities"/>
					<property name="bridgeErrorHandler" type="boolean" display="Bridge Error Handler" label="consumer" description="Allows for bridging the consumer to the Camel routing Error Handler, which mean any exceptions occurred while the consumer is trying to pickup incoming messages, or the likes, will now be processed as a message and handled by the routing Error Handler. By default the consumer will use the org.apache.camel.spi.ExceptionHandler to deal with exceptions, that will be logged at WARN or ERROR level and ignored."/>
					<property name="exceptionHandler" type="java:org.apache.camel.spi.ExceptionHandler" display="Exception Handler" label="consumer,advanced" description="To let the consumer use a custom ExceptionHandler. Notice if the option bridgeErrorHandler is enabled then this option is not in use. By default the consumer will deal with exceptions, that will be logged at WARN or ERROR level and ignored."/>
					<property name="exchangePattern" type="enum:org.apache.camel.ExchangePattern(InOnly,InOut,InOptionalOut)" display="Exchange Pattern" label="consumer,advanced" description="Sets the exchange pattern when the consumer creates an exchange."/>
					<property name="lazyStartProducer" type="boolean" display="Lazy Start Producer" label="producer" description="Whether the producer should be started lazy (on the first message). By starting lazy you can use this to allow CamelContext and routes to startup in situations where a producer may otherwise fail during starting and cause the route to fail being started. By deferring this startup to be lazy then the startup failure can be handled during routing messages via Camel's routing error handlers. Beware that when the first message is processed then creating and starting the producer may take a little time and prolong the total processing time of the processing."/>
					<property name="synchronous" type="boolean" display="Synchronous" label="advanced" description="Sets whether synchronous processing should be strictly used, or Camel is allowed to use asynchronous processing (if supported)."/>
				</definition>
				<definition name="scheduled" abstract="true" extends="model:endpoint">
					<property name="backoffMultiplier" type="int" display="Backoff Multiplier" label="consumer,scheduler" description="To let the scheduled polling consumer backoff if there has been a number of subsequent idles/errors in a row. The multiplier is then the number of polls that will be skipped before the next actual attempt is happening again. When this option is in use then backoffIdleThreshold and/or backoffErrorThreshold must also be configured."/>
					<property name="backoffIdleThreshold" type="int" display="Backoff Idle Threshold" label="consumer,scheduler" description="The number of subsequent idle polls that should happen before the backoffMultipler should kick-in."/>
					<property name="backoffErrorThreshold" type="int" display="Backoff Error Threshold" label="consumer,scheduler" description="The number of subsequent error polls (failed due some error) that should happen before the backoffMultipler should kick-in."/>
					<property name="delay" type="long" display="Delay" label="consumer,scheduler" description="Milliseconds before the next poll. You can also specify time values using units, such as 60s (60 seconds), 5m30s (5 minutes and 30 seconds), and 1h (1 hour)."/>
					<property name="greedy" type="boolean" display="Greedy" label="consumer,scheduler" description="If greedy is enabled, then the ScheduledPollConsumer will run immediately again, if the previous run polled 1 or more messages."/>
					<property name="initialDelay" type="long" display="Initial Delay" label="consumer,scheduler" description="Milliseconds before the first poll starts. You can also specify time values using units, such as 60s (60 seconds), 5m30s (5 minutes and 30 seconds), and 1h (1 hour)."/>
					<property name="pollStrategy" type="java:org.apache.camel.spi.PollingConsumerPollStrategy" display="Poll Strategy" label="consumer,advanced" description="A pluggable org.apache.camel.PollingConsumerPollingStrategy allowing you to provide your custom implementation to control error handling usually occurred during the poll operation before an Exchange have been created and being routed in Camel."/>
					<property name="runLoggingLevel" type="enum:org.apache.camel.LoggingLevel(TRACE,DEBUG,INFO,WARN,ERROR,OFF)" display="Run Logging Level" label="consumer,scheduler" description="The consumer logs a start/complete log line when it polls. This option allows you to configure the logging level for that."/>
					<property name="scheduler" type="enum:org.apache.camel.spi.ScheduledPollConsumerScheduler(none,spring,quartz2)" display="Scheduler" label="consumer,scheduler" description="To use a cron scheduler from either camel-spring or camel-quartz2 component"/>
					<property name="schedulerProperties" type="map(string,object)" display="Scheduler Properties" label="consumer,scheduler" description="To configure additional properties when using a custom scheduler or any of the Quartz2, Spring based scheduler."/>
					<property name="scheduledExecutorService" type="java:java.util.concurrent.ScheduledExecutorService" display="Scheduled Executor Service" label="consumer,scheduler" description="Allows for configuring a custom/shared thread pool to use for the consumer. By default each consumer has its own single threaded thread pool."/>
					<property name="sendEmptyMessageWhenIdle" type="boolean" display="Send Empty Message When Idle" label="consumer" description="If the polling consumer did not poll any files, you can enable this option to send an empty message (no body) instead."/>
					<property name="startScheduler" type="boolean" display="Start Scheduler" label="consumer,scheduler" description="Whether the scheduler should be auto started."/>
					<property name="timeUnit" type="enum:java.util.concurrent.TimeUnit(NANOSECONDS,MICROSECONDS,MILLISECONDS,SECONDS,MINUTES,HOURS,DAYS)" display="Time Unit" label="consumer,scheduler" description="Time unit for initialDelay and delay options."/>
					<property name="useFixedDelay" type="boolean" display="Use Fixed Delay" label="consumer,scheduler" description="Controls if fixed delay or fixed rate is used. See ScheduledExecutorService in JDK for details."/>
				</definition>

				<!--
				   - Expressions
				   -->
				<xsl:comment>&#13;      - Expressions&#13;      </xsl:comment>
				<definition name="expression" abstract="true">
					<property name="id" display="Id" type="string" />
					<property name="expression" display="Expression" type="string" required="true" />
					<property name="trim" display="Trim" type="boolean" />
				</definition>

				<!--
				   - Rest verbs
				   -->
				<xsl:comment>&#13;      - Rest verbs&#13;      </xsl:comment>
				<definition name="verb" label="rest" abstract="true" extends="model:node">
					<property name="apiDocs" type="boolean" display="Api Docs" description="Whether to include or exclude the VerbDefinition in API documentation. The default value is true."/>
					<property name="bindingMode" type="enum:org.apache.camel.model.rest.RestBindingMode(auto,json,json_xml,off,xml)" display="Binding Mode" description="Sets the binding mode to use. This option will override what may be configured on a parent level The default value is auto"/>
					<property name="clientRequestValidation" type="boolean" display="Client Request Validation" description="Whether to enable validation of the client request to check whether the Content-Type and Accept headers from the client is supported by the Rest-DSL configuration of its consumes/produces settings. This can be turned on, to enable this check. In case of validation error, then HTTP Status codes 415 or 406 is returned. The default value is false."/>
					<property name="consumes" type="string" display="Consumes" description="To define the content type what the REST service consumes (accept as input), such as application/xml or application/json. This option will override what may be configured on a parent level"/>
					<property name="enableCORS" type="boolean" display="Enable CORS" description="Whether to enable CORS headers in the HTTP response. This option will override what may be configured on a parent level The default value is false."/>
					<property name="method" type="string" display="Method" description="The HTTP verb such as GET, POST, DELETE, etc."/>
					<property name="outType" type="string" display="Out Type" description="Sets the class name to use for binding from POJO to output for the outgoing data This option will override what may be configured on a parent level The canonical name of the class of the input data. Append a to the end of the canonical name if you want the input to be an array type."/>
					<property name="produces" type="string" display="Produces" description="To define the content type what the REST service produces (uses for output), such as application/xml or application/json This option will override what may be configured on a parent level"/>
					<property name="routeId" type="string" display="Route Id" description="The route id this rest-dsl is using (read-only)"/>
					<property name="skipBindingOnErrorCode" type="boolean" display="Skip Binding On Error Code" description="Whether to skip binding on output if there is a custom HTTP error code header. This allows to build custom error messages that do not bind to json / xml etc, as success messages otherwise will do. This option will override what may be configured on a parent level"/>
					<property name="toOrRoute" type="model:node" display="To Or Route" required="true" kind="element" description="To route from this REST service to a Camel endpoint, or an inlined route"/>
					<property name="type" type="string" display="Type" description="Sets the class name to use for binding from input to POJO for the incoming data This option will override what may be configured on a parent level. The canonical name of the class of the input data. Append a to the end of the canonical name if you want the input to be an array type."/>
					<property name="uri" type="string" display="Uri" description="Uri template of this REST service such as /{id}."/>
				</definition>

				<!--
				  - LoadBalancer
				  -->
				<xsl:comment>&#13;      - Load balancers&#13;      </xsl:comment>
				<definition name="loadBalancer" label="rest" abstract="true" extends="model:identified" description="Balances message processing among a number of nodes."/>
			</definitions>
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
			<xsl:comment>&#13;    - EIPs&#13;      </xsl:comment>
			<eips>
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
						<xsl:element name="eip">
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
							<xsl:attribute name="name">
								<xsl:value-of select="local-name(.)"/>
							</xsl:attribute>
							<xsl:if test="$parent != ''">
								<xsl:attribute name="extends"><xsl:value-of select="concat('model:', $parent)"/></xsl:attribute>
							</xsl:if>
							<xsl:attribute name="display">
								<xsl:value-of select="model/title"/>
							</xsl:attribute>
							<xsl:attribute name="label">
								<xsl:value-of select="model/label"/>
							</xsl:attribute>
							<xsl:if test="model/deprecated/text() = 'true'">
								<xsl:attribute name="deprecated">true</xsl:attribute>
							</xsl:if>
							<xsl:if test="string-length(model/firstVersion/text())&gt;0">
								<xsl:attribute name="since">
									<xsl:value-of select="model/firstVersion"/>
								</xsl:attribute>
							</xsl:if>
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
			</eips>
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
