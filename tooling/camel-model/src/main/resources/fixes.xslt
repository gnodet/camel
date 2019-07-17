<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dyn="http://exslt.org/dynamic" version="1.0"
                xmlns:xs="http://www.w3.org/1999/XSL/Transform"
                extension-element-prefixes="dyn">
    <xsl:output indent="yes" method="xml"/>

    <xsl:template match="/model/dataFormats/dataFormat[@name='avro']">
        <dataFormat name="avro" display="Avro" label="dataformat,transformation" extends="model:dataFormat" maven="org.apache.camel:camel-avro:3.0.0-SNAPSHOT" javaType="org.apache.camel.model.dataformat.AvroDataFormat" since="2.14.0" description="The Avro data format is used for serialization and deserialization of messages using Apache Avro binary dataformat.">
            <property name="instanceClassName" type="string" display="Instance Class Name" required="true" description="Class name to use for marshal and unmarshalling"/>
            <property name="schema" type="object" display="Schema" description="Avro Schema or GenericContainer object"/>
        </dataFormat>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='bindy']/@name">
        <xsl:attribute name="name"><xsl:value-of select="'bindy'"/></xsl:attribute>
        <xsl:attribute name="javaType"><xsl:value-of select="'org.apache.camel.model.dataformat.BindyDataFormat'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='bindy']/property[@name='classType']/@type">
        <xsl:attribute name="type"><xsl:value-of select="'class'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='boon']/property[@name='unmarshalTypeName']">
        <property name="unmarshalType" type="class" display="Unmarshal Type" required="true" description="Java type to use when unmarshalling"/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='cbor']/property[@name='unmarshalTypeName']">
        <property name="unmarshalType" type="class" display="Unmarshal Type" description="Java type to use when unmarshalling"/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='customDataFormat']/@name">
        <xsl:attribute name="name"><xsl:value-of select="'custom'"/></xsl:attribute>
        <xsl:attribute name="javaType"><xsl:value-of select="'org.apache.camel.model.dataformat.CustomDataFormat'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='customDataFormat']/property[@name='ref']">
        <property name="dataFormat" type="java:org.apache.camel.spi.DataFormat"  display="Data Format" required="true" description="Instance or reference to the custom org.apache.camel.spi.DataFormat to lookup from the Camel registry."/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='json']/@name">
        <xsl:attribute name="name"><xsl:value-of select="'json'"/></xsl:attribute>
        <xsl:attribute name="javaType"><xsl:value-of select="'org.apache.camel.model.dataformat.JsonDataFormat'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='hl7']">
        <dataFormat name="hl7" display="HL7" label="dataformat,transformation,hl7" extends="model:dataFormat" maven="org.apache.camel:camel-hl7:3.0.0-SNAPSHOT" javaType="org.apache.camel.model.dataformat.HL7DataFormat" since="2.0.0" description="The HL7 data format can be used to marshal or unmarshal HL7 (Health Care) model objects.">
            <property name="validate" type="boolean" display="Validate" description="Whether to validate the HL7 message Is by default true."/>
            <property name="parser" type="object" display="Parser" description="To use a custom HL7 parser."/>
        </dataFormat>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='jacksonxml']/property[@name='unmarshalTypeName']">
        <property name="unmarshalType" type="class" display="Unmarshal Type" description="Java type to use when unmarshalling"/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='json']/property[@name='unmarshalTypeName']">
        <property name="unmarshalType" type="class" display="Unmarshal Type" description="Java type to use when unmarshalling"/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='mime-multipart']/@name">
        <xsl:attribute name="name">mimeMultipart</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='protobuf']">
        <dataFormat name="protobuf" display="Protobuf" label="dataformat,transformation" extends="model:dataFormat" maven="org.apache.camel:camel-protobuf:3.0.0-SNAPSHOT" javaType="org.apache.camel.model.dataformat.ProtobufDataFormat" since="2.2.0" description="The Protobuf data format is used for serializing between Java objects and the Google Protobuf protocol.">
            <property name="contentTypeFormat" type="string" display="Content Type Format" description="Defines a content type format in which protobuf message will be serialized/deserialized from(to) the Java been. The format can either be native or json for either native protobuf or json fields representation. The default value is native."/>
            <property name="instanceClass" type="string" display="Instance Class" description="Name of class to use when unmarshalling"/>
            <property name="defaultInstance" type="java:com.google.protobuf.Message" display="Default Instance" description="The message instance to use when unmarshalling"/>
        </dataFormat>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='soapjaxb']/property[@name='elementNameStrategyRef']">
        <property name="elementNameStrategy" type="java:org.apache.camel.dataformat.soap.name.ElementNameStrategy" display="Element Name Strategy" description="The element name strategy is used for two purposes. The first is to find a xml element name for a given object and soap action when marshaling the object into a SOAP message. The second is to find an Exception class for a given soap fault name. The following three element strategy class name is provided out of the box. QNameStrategy - Uses a fixed qName that is configured on instantiation. Exception lookup is not supported TypeNameStrategy - Uses the name and namespace from the XMLType annotation of the given type. If no namespace is set then package-info is used. Exception lookup is not supported ServiceInterfaceStrategy - Uses information from a webservice interface to determine the type name and to find the exception class for a SOAP fault All three classes is located in the package name org.apache.camel.dataformat.soap.name If you have generated the web service stub code with cxf-codegen or a similar tool then you probably will want to use the ServiceInterfaceStrategy. In the case you have no annotated service interface you should use QNameStrategy or TypeNameStrategy."/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='thrift']">
        <dataFormat name="thrift" display="Thrift" label="dataformat,transformation" extends="model:dataFormat" maven="org.apache.camel:camel-thrift:3.0.0-SNAPSHOT" javaType="org.apache.camel.model.dataformat.ThriftDataFormat" since="2.20.0" description="The Thrift data format is used for serialization and deserialization of messages using Apache Thrift binary dataformat.">
            <property name="contentTypeFormat" type="string" display="Content Type Format" description="Defines a content type format in which thrift message will be serialized/deserialized from(to) the Java been. The format can either be native or json for either native binary thrift, json or simple json fields representation. The default value is binary."/>
            <property name="instanceClass" type="string" display="Instance Class" description="Name of class to use when unmarshalling"/>
            <property name="defaultInstance" type="java:org.apache.thrift.TBase" display="Default Instance" description="The TBase instance to use when unmarshalling"/>
        </dataFormat>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='tidyMarkup']/property[@name='dataObjectType']">
        <property name="dataObjectType" type="class" display="Data Object Type" description="What data type to unmarshal as, can either be org.w3c.dom.Node or java.lang.String. Is by default org.w3c.dom.Node"/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='xmlrpc']/@name">
        <xsl:attribute name="name"><xsl:value-of select="'xmlrpc'"/></xsl:attribute>
        <xsl:attribute name="javaType"><xsl:value-of select="'org.apache.camel.model.dataformat.XmlRpcDataFormat'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='yaml']/@name">
        <xsl:attribute name="name"><xsl:value-of select="'yaml'"/></xsl:attribute>
        <xsl:attribute name="javaType"><xsl:value-of select="'org.apache.camel.model.dataformat.YAMLDataFormat'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='yaml']/property[@name='unmarshalTypeName']">
        <property name="unmarshalType" type="class" display="Unmarshal Type" description="Java type to use when unmarshalling"/>
    </xsl:template>
    <xsl:template match="/model/dataFormats/dataFormat[@name='secureXML']/property[@name='keyOrTrustStoreParametersRef']">
        <property name="keyOrTrustStoreParameters" type="java:org.apache.camel.support.jsse.KeyStoreParameters" display="KeyStoreParameters" description="The element name strategy is used for two purposes. The first is to find a xml element name for a given object and soap action when marshaling the object into a SOAP message. The second is to find an Exception class for a given soap fault name. The following three element strategy class name is provided out of the box. QNameStrategy - Uses a fixed qName that is configured on instantiation. Exception lookup is not supported TypeNameStrategy - Uses the name and namespace from the XMLType annotation of the given type. If no namespace is set then package-info is used. Exception lookup is not supported ServiceInterfaceStrategy - Uses information from a webservice interface to determine the type name and to find the exception class for a SOAP fault All three classes is located in the package name org.apache.camel.dataformat.soap.name If you have generated the web service stub code with cxf-codegen or a similar tool then you probably will want to use the ServiceInterfaceStrategy. In the case you have no annotated service interface you should use QNameStrategy or TypeNameStrategy."/>
        <property name="namespaces" type="map(string,string)" display="Namespaces" description="XML Namespaces of prefix -> uri mappings" />
    </xsl:template>

    <xsl:template match="/model/languages">
        <languages>
            <xsl:apply-templates select="*"/>
            <language name="language" display="Language" label="language,core" extends="model:expression" maven="org.apache.camel:camel-base:3.0.0-SNAPSHOT" javaType="xxx.LanguageLanguage" description="To use the specified language in Camel expressions or predicates.">
                <property name="language" type="string" display="Language" description="The name of the language to use"/>
            </language>
        </languages>
    </xsl:template>
    <xsl:template match="/model/languages/language/@extends">
        <xsl:attribute name="extends">model:expression</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='bean']/@javaType">
        <xsl:attribute name="javaType"><xs:value-of select="'xxx.MethodCallLanguage'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='bean']/property[@name='beanType']">
        <property name="beanType" type="class" display="Bean Type" description="Class of the bean to use"/>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='bean']/property[@name='ref']">
        <property name="bean" type="object" display="Bean" description="The bean to use"/>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='jsonpath']/property[@name='resultType']">
        <property name="resultType" type="class" display="Result Type" description="Sets the class of the result type (type from output)"/>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='simple']/property[@name='resultType']">
        <property name="resultType" type="class" display="Result Type" description="Sets the class of the result type (type from output)"/>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='spel']/@javaType">
        <xsl:attribute name="javaType"><xs:value-of select="'xxx.SpELLanguage'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='tokenize']/@javaType">
        <xsl:attribute name="javaType"><xs:value-of select="'xxx.TokenizerLanguage'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='xtokenize']/@javaType">
        <xsl:attribute name="javaType"><xs:value-of select="'xxx.XMLTokenizerLanguage'"/></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='xpath']/property[@name='resultType']">
        <property name="resultType" type="class" display="Result Type" description="Sets the class of the result type (type from output)"/>
        <property name="namespaces" type="map(string,string)" display="Namespaces" description="XML Namespaces of prefix -> uri mappings" />
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='xquery']/property[@name='type']">
        <property name="resultType" type="class" display="Result Type" description="Sets the class of the result type (type from output)"/>
        <property name="namespaces" type="map(string,string)" display="Namespaces" description="XML Namespaces of prefix -> uri mappings" />
    </xsl:template>
    <xsl:template match="/model/languages/language[@name='xtokenize']">
        <xsl:element name="language">
            <xsl:apply-templates select="@*|*"/>
            <property name="namespaces" type="map(string,string)" display="Namespaces" description="XML Namespaces of prefix -> uri mappings" />
        </xsl:element>
    </xsl:template>

    <xsl:template match="/model/processors">
        <processors>
            <xsl:apply-templates select="* | /model/structs/struct[@name='serviceCall' or @name='route']"/>
        </processors>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='serviceCall']">
        <xsl:element name="processor">
            <xsl:apply-templates select="@*|*"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='route']">
        <xsl:element name="processor">
            <xsl:apply-templates select="@*|*[not(@name='input' or @name='outputs')]"/>
<!--            <property name="rest" type="model:rest" />-->
            <xsl:apply-templates select="property[@name='input']"/>
            <property name="inputType" type="model:inputType" required="false"/>
            <property name="outputType" type="model:outputType" required="false"/>
            <xsl:apply-templates select="property[@name='outputs']"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='aggregate']/property[@name='aggregateControllerRef']">
        <property name="aggregationController" type="java:org.apache.camel.spi.AggregateController" />
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='aggregate']/property[@name='aggregationRepositoryRef']">
        <property name="aggregationRepository" type="java:org.apache.camel.spi.AggregationRepository" />
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='aggregate']/property[@name='executorServiceRef']" priority="2">
        <property name="executorService" type="java:java.util.concurrent.ExecutorService"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='aggregate']/property[@name='timeoutCheckerExecutorServiceRef']">
        <property name="timeoutCheckerExecutorService" type="java:java.util.concurrent.ScheduledExecutorService"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='bean']/property[@name='ref']">
        <property name="bean" type="object" display="Bean" description="Sets the bean to use"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='bean']/property[@name='beanType']/@type">
        <xsl:attribute name="type">class</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='claimCheck']/property[@name='operation']/@type">
        <xsl:attribute name="type">enum:ClaimCheckOperation(Get,GetAndRemove,Pop,Push,Set)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='convertBodyTo']/property[@name='charset']/@type">
        <xsl:attribute name="type">java:java.nio.charset.Charset</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='convertBodyTo']/property[@name='type']/@type">
        <xsl:attribute name="type">class</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='doTry']">
        <xsl:element name="processor">
            <xsl:apply-templates select="@* | node()"/>
            <property name="catchClauses" type="list(model:doCatch)"/>
            <property name="finallyClause" type="model:doFinally"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='hystrix']/property[@name='hystrixConfigurationRef']">
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='idempotentConsumer']/property[@name='messageIdRepositoryRef']">
        <property name="messageIdRepository" type="java:org.apache.camel.spi.IdempotentRepository"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='log']/property[@name='loggerRef']">
        <property name="logger" type="java:org.slf4j.Logger"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='multicast']/property[@name='onPrepareRef']">
        <property name="onPrepare" type="model:processor" display="On Prepare" description="Uses the Processor when preparing the org.apache.camel.Exchange to be send. This can be used to deep-clone messages that should be send, or any custom logic needed before the exchange is send."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='onCompletion']/property[@name='mode']/@type">
        <xsl:attribute name="type">enum:OnCompletionMode(AfterConsumer,BeforeConsumer)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='onException']/property[@name='onExceptionOccurredRef']">
        <property name="onExceptionOccurred" type="model:processor" display="On Exception Occurred" description="Sets a processor that should be processed just after an exception occurred. Can be used to perform custom logging about the occurred exception at the exact time it happened. Important: Any exception thrown from this processor will be ignored."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='onException']/property[@name='onRedeliveryRef']">
        <property name="onRedelivery" type="model:processor" display="On Redelivery" description="Sets a processor that should be processed before a redelivery attempt. Can be used to change the org.apache.camel.Exchange before its being redelivered."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='onException']/property[@name='redeliveryPolicyRef']">
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='onException']">
        <xsl:element name="processor">
            <xsl:apply-templates select="@* | node()"/>
            <property name="routeScoped" type="boolean"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='policy']/property[@name='ref']">
        <property name="type" type="class" display="Type" description="Sets a policy type that this definition should scope within."/>
        <property name="instance" type="java:org.apache.camel.spi.Policy" display="Policy" description="Sets a policy that this definition should scope within."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='process']/property[@name='ref']">
        <property name="processor" type="java:org.apache.camel.Processor" display="Processor" description="The Processor to use."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='resequence']/property[@name='resequencerConfig']/@type">
        <xsl:attribute name="type">model:resequencerConfig</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='removeHeaders' or @name='removeProperties']/property[@name='excludePattern']">
        <property name="excludePatterns" type="string[]" display="Exclude Patterns" description="Names or patterns of headers to not remove. The pattern is matched in the following order: 1 = exact match 2 = wildcard (pattern ends with a and the name starts with the pattern) 3 = regular expression (all of above is case in-sensitive)."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='saga']/property[@name='compensation' or @name='completion']/@type">
        <xsl:attribute name="type">model:sagaActionUri</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='saga']/property[@name='completionMode']/@type">
        <xsl:attribute name="type">enum:SagaCompletionMode(AUTO,MANUAL)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='saga']/property[@name='option']/@type">
        <xsl:attribute name="type">list(model:sagaOption)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='saga']/property[@name='propagation']/@type">
        <xsl:attribute name="type">enum:SagaPropagation(MANDATORY,NEVER,NOT_SUPPORTED,REQUIRED,REQUIRES_NEW,SUPPORTS)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='serviceCall']/property[@name='configurationRef']">
        <property name="configuration" type="model:serviceCallConfiguration" display="Configuration" description="ServiceCall configuration to use"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='sort']">
        <xsl:element name="processor">
            <xsl:apply-templates select="@*" />
            <xsl:attribute name="param">T</xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='sort']/property[@name='comparatorRef']">
        <property name="comparator" type="java:java.util.Comparator&lt;? super T>" display="Comparator" description="Uses the Processor when preparing the org.apache.camel.Exchange to be send. This can be used to deep-clone messages that should be send, or any custom logic needed before the exchange is send."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='split']/property[@name='onPrepareRef']">
        <property name="onPrepare" type="model:processor" display="On Prepare" description="Uses the Processor when preparing the org.apache.camel.Exchange to be send. This can be used to deep-clone messages that should be send, or any custom logic needed before the exchange is send."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='throwException']/property[@name='ref']">
        <property name="exception" type="java:java.lang.Exception" />
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='throwException']/property[@name='exceptionType']/@type">
        <xsl:attribute name="type">class</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='to']/property[@name='uri']">
        <property name="uri" type="model:endpoint" display="Uri" required="true" description="The uri of the endpoint to send to. The uri can be dynamic computed using the org.apache.camel.language.simple.SimpleLanguage expression."/>
        <property name="endpoint" type="java:org.apache.camel.Endpoint" display="Endpoint" description="The endpoint to send to."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='toD']/property[@name='uri']">
        <property name="uri" type="model:endpoint" display="Uri" required="true" description="The uri of the endpoint to send to. The uri can be dynamic computed using the org.apache.camel.language.simple.SimpleLanguage expression."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='transacted']/property[@name='ref']">
        <property name="type" type="class" display="Type" description="Sets a policy type that this definition should scope within."/>
        <property name="instance" type="java:org.apache.camel.spi.Policy" display="Policy" description="The policy to use."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='wireTap']/property[@name='uri']">
        <property name="uri" type="model:endpoint" display="Uri" required="true" description="The uri of the endpoint to send to. The uri can be dynamic computed using the org.apache.camel.language.simple.SimpleLanguage expression."/>
        <property name="endpoint" type="java:org.apache.camel.Endpoint" display="Endpoint" description="The endpoint to send to."/>
        <property name="headers" type="list(model:setHeader)" />
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='wireTap']/property[@name='onPrepareRef']">
        <property name="onPrepare" type="model:processor" display="On Prepare" description="Uses the Processor when preparing the org.apache.camel.Exchange to be send. This can be used to deep-clone messages that should be send, or any custom logic needed before the exchange is send."/>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='wireTap']/property[@name='processorRef']">
        <property name="newExchange" type="model:processor" display="New Exchange" description="Processor to use for creating a new body as the message to use for wire tapping"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor/property[@name='executorServiceRef']">
        <property name="executorService" type="java:java.util.concurrent.ExecutorService"/>
    </xsl:template>
    <xsl:template match="/model/processors/processor/property[@name='strategyRef']">
        <property name="aggregationStrategy" type="java:org.apache.camel.AggregationStrategy" display="Aggregation Strategy" description="Aggregation strategy to use."/>
    </xsl:template>

    <xsl:template match="/model/endpoints/endpoint[@name='atmosphere-websocket']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.component.atmosphere.websocket.AtmosphereWebsocketComponent</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='fhir']/property[@name='apiName']/@type">
        <xsl:attribute name="type">enum:FhirApiName(CAPABILITIES:capabilities,CREATE:create,DELETE:delete,HISTORY:history,LOAD_PAGE:load-page,META:meta,PATCH:patch,READ:read,SEARCH:search,TRANSACTION:transaction,UPDATE:update,VALIDATE:validate)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='google-drive']/property[@name='apiName']/@type">
        <xsl:attribute name="type">enum:GoogleDriveApiName(DRIVE_ABOUT:drive-about,DRIVE_APPS:drive-apps,DRIVE_CHANGES:drive-changes,DRIVE_CHANNELS:drive-channels,DRIVE_CHILDREN:drive-children,DRIVE_COMMENTS:drive-comments,DRIVE_FILES:drive-files,DRIVE_PARENTS:drive-parents,DRIVE_PERMISSIONS:drive-permissions,DRIVE_PROPERTIES:drive-properties,DRIVE_REALTIME:drive-realtime,DRIVE_REPLIES:drive-replies,DRIVE_REVISIONS:drive-revisions)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='zookeeper-master']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.component.zookeepermaster.ZookeeperMasterComponent</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='salesforce']/property[@name='operationName']/@type">
        <xsl:attribute name="type">enum:OperationName(GET_VERSIONS:getVersions,GET_RESOURCES:getResources,GET_GLOBAL_OBJECTS:getGlobalObjects,GET_BASIC_INFO:getBasicInfo,GET_DESCRIPTION:getDescription,GET_SOBJECT:getSObject,CREATE_SOBJECT:createSObject,UPDATE_SOBJECT:updateSObject,DELETE_SOBJECT:deleteSObject,GET_SOBJECT_WITH_ID:getSObjectWithId,UPSERT_SOBJECT:upsertSObject,DELETE_SOBJECT_WITH_ID:deleteSObjectWithId,GET_BLOB_FIELD:getBlobField,QUERY:query,QUERY_MORE:queryMore,QUERY_ALL:queryAll,SEARCH:search,APEX_CALL:apexCall,RECENT:recent,CREATE_JOB:createJob,GET_JOB:getJob,CLOSE_JOB:closeJob,ABORT_JOB:abortJob,CREATE_BATCH:createBatch,GET_BATCH:getBatch,GET_ALL_BATCHES:getAllBatches,GET_REQUEST:getRequest,GET_RESULTS:getResults,CREATE_BATCH_QUERY:createBatchQuery,GET_QUERY_RESULT_IDS:getQueryResultIds,GET_QUERY_RESULT:getQueryResult,GET_RECENT_REPORTS:getRecentReports,GET_REPORT_DESCRIPTION:getReportDescription,EXECUTE_SYNCREPORT:executeSyncReport,EXECUTE_ASYNCREPORT:executeAsyncReport,GET_REPORT_INSTANCES:getReportInstances,GET_REPORT_RESULTS:getReportResults,LIMITS:limits,APPROVAL:approval,APPROVALS:approvals,COMPOSITE_TREE:composite-tree,COMPOSITE_BATCH:composite-batch,COMPOSITE:composite)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='spark-rest']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.component.sparkrest.SparkRestComponent</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='splunk']/property[@name='sslProtocol']/@type">
        <xsl:attribute name="type">enum:com.splunk.SSLSecurityProtocol(TLSv1_2:TLSv1.2,TLSv1_1:TLSv1.1,TLSv1:TLSv1,SSLv3:SSLv3)</xsl:attribute>
    </xsl:template>

    <xsl:template match="/model/structs">
        <structs>
            <struct name="dataFormat" display="Data Format" abstract="true" generate="false" extends="model:identified" javaType="org.apache.camel.model.dataformats.DataFormatDefinition" label="abstract">
                <property name="contentTypeHeader" type="boolean" display="Content Type Header" description="Whether the data format should set the Content-Type header with the type from the data format if the data format is capable of doing so. For example application/xml for data formats marshalling to XML, or application/json for data formats marshalling to JSon etc."/>
            </struct>
            <struct name="identified" display="Identified" abstract="true" generate="false" javaType="org.apache.camel.model.IdentifiedType" label="abstract">
                <property name="id" type="string" display="Id" description="Sets the value of the id property."/>
            </struct>
            <struct name="node" display="Node" abstract="true" generate="false" javaType="org.apache.camel.model.OptionalIdentifiedDefinition" label="abstract">
                <property name="id" type="string" display="Id" description="Sets the id of this node" required="false"/>
                <property name="description" type="model:description" display="Description" description="Sets the description of this node" required="false"/>
            </struct>
            <struct name="processor" display="Processor" abstract="true" generate="false" extends="model:node" javaType="org.apache.camel.model.processors.ProcessorDefinition" label="abstract"/>
            <struct name="loadBalancer" display="Load Balancer" abstract="true" generate="false" extends="model:identified" description="Balances message processing among a number of nodes." javaType="org.apache.camel.model.loadbalancers.LoadBalancerDefinition" label="abstract" />
            <struct name="endpoint" display="Endpoint" abstract="true" generate="false" javaType="org.apache.camel.model.endpoints.EndpointProducerBuilder"/>
            <struct name="resequencerConfig" display="Resequencer Config" abstract="true" generate="false" javaType="org.apache.camel.model.structs.ResequencerConfig" label="abstract" />
            <xsl:apply-templates select="struct[not(starts-with(@javaType,'org.apache.camel.spring.'))][@name != 'serviceCall' and @name != 'route']"/>
            <struct name="sagaActionUri" javaType="org.apache.camel.model.structs.SagaActionUriDefinition" label="eip,routing">
                <property name="uri" type="string"/>
            </struct>
            <struct name="sagaOption" javaType="org.apache.camel.model.structs.SagaOptionDefinition" label="eip,routing">
                <property name="optionName" type="string"/>
                <property name="expression" type="model:expression"/>
            </struct>
            <struct name="securityDefinition" abstract="true" javaType="org.apache.camel.model.rest.RestSecurityDefinition" display="Security Definition" label="rest,security" description="Security definition">
                <property name="key" type="string" required="true"/>
                <property name="description" type="string"/>
            </struct>
            <struct name="transformer" javaType="org.apache.camel.model.transformer.TransformerDefinition" label="transformation">
                <property name="scheme" type="string" description="Set a scheme name supported by the transformer. If you specify 'csv', the transformer will be picked up for all of 'csv' from/to Java transformation. Note that the scheme matching is performed only when no exactly matched transformer exists."/>
                <property name="fromType" type="class" description="Set the 'from' data type using Java class."/>
                <property name="fromType" type="java:org.apache.camel.spi.DataType" description="Set the 'from' data type name. If you specify 'xml:XYZ', the transformer will be picked up if source type is 'xml:XYZ'. If you specify just 'xml', the transformer matches with all of 'xml' source type like 'xml:ABC' or 'xml:DEF'."/>
                <property name="toType" type="class" description="Set the 'to' data type using Java class."/>
                <property name="toType" type="java:org.apache.camel.spi.DataType" description="Set the 'to' data type name. If you specify 'json:XYZ', the transformer will be picked up if source type is 'json:XYZ'. If you specify just 'json', the transformer matches with all of 'json' source type like 'json:ABC' or 'json:DEF'."/>
            </struct>
            <struct name="customTransformer" extends="model:transformer" javaType="org.apache.camel.model.transformer.CustomTransformerDefinition" label="validation">
                <property name="transformer" type="java:org.apache.camel.spi.Transformer"/>
                <property name="type" type="class"/>
            </struct>
            <struct name="dataFormatTransformer" extends="model:transformer" javaType="org.apache.camel.model.transformer.DataFormatTransformerDefinition" label="validation">
                <property name="dataFormat" type="model:dataFormat"/>
            </struct>
            <struct name="endpointTransformer" extends="model:transformer" javaType="org.apache.camel.model.transformer.EndpointTransformerDefinition" label="validation">
                <property name="uri" type="model:endpoint"/>
            </struct>
            <struct name="validator" javaType="org.apache.camel.model.validator.ValidatorDefinition" label="validation">
                <property name="type" type="class" description="Set the data type using Java class."/>
                <property name="type" type="java:org.apache.camel.spi.DataType" description="Set the data type name. If you specify 'xml:XYZ', the validator will be picked up if message type is 'xml:XYZ'. If you specify just 'xml', the validator matches with all of 'xml' message type like 'xml:ABC' or 'xml:DEF'."/>
            </struct>
            <struct name="customValidator" extends="model:validator" javaType="org.apache.camel.model.validator.CustomValidatorDefinition" label="validation">
                <property name="validator" type="java:org.apache.camel.spi.Validator"/>
            </struct>
            <struct name="endpointValidator" extends="model:validator" javaType="org.apache.camel.model.validator.EndpointValidatorDefinition" label="validation">
                <property name="uri" type="model:endpoint"/>
            </struct>
            <struct name="predicateValidator" extends="model:validator" javaType="org.apache.camel.model.validator.PredicateValidatorDefinition" label="validation">
                <property name="expression" type="model:expression"/>
            </struct>
        </structs>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='apiKey' or @name='basicAuth' or @name='oauth2']/@name">
        <xsl:attribute name="name"><xsl:value-of select="."/></xsl:attribute>
        <xsl:attribute name="extends">model:securityDefinition</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='batch-config' or @name='stream-config']/@name">
        <xsl:attribute name="name"><xsl:value-of select="."/></xsl:attribute>
        <xsl:attribute name="extends">model:resequencerConfig</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='apiKey' or @name='basicAuth' or @name='oauth2']/property[@name='key' or @name='description']">
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='customDataFormat']">
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='expression']">
        <xsl:element name="struct">
            <xsl:apply-templates select="@*" />
            <xsl:attribute name="generate">false</xsl:attribute>
            <xsl:attribute name="abstract">true</xsl:attribute>
            <xsl:apply-templates select="node()" />
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='expression']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.model.languages.ExpressionDefinition</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='language']">
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='loadBalancer']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.model.loadbalancers.LoadBalancerDefinition</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='method']">
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='packageScan']/property[@name='package']/@name">
        <xsl:attribute name="name">packages</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='processor']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.model.processors.ProcessorDefinition</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='rest']">
        <xsl:element name="struct">
            <xsl:apply-templates select="@*" />
            <xsl:attribute name="extends">java:org.apache.camel.model.rest.AbstractRestDefinition</xsl:attribute>
            <xsl:apply-templates select="node()" />
            <property name="verbs" type="list(model:verb)" />
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='securityDefinitions']/property[@name='securityDefinitions']/@type">
        <xsl:attribute name="type">model:securityDefinition</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='verb']">
        <xsl:element name="struct">
            <xsl:apply-templates select="@*" />
            <xsl:attribute name="extends">java:org.apache.camel.model.rest.AbstractVerbDefinition</xsl:attribute>
            <xsl:apply-templates select="node()" />
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='transformers']/property[@name='transformers']/@type">
        <xsl:attribute name="type">list(model:transformer)</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/structs/struct[@name='validators']/property[@name='validators']/@type">
        <xsl:attribute name="type">list(model:validator)</xsl:attribute>
    </xsl:template>


    <xsl:template match="/ | @* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
