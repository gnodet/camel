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
            <language name="language" display="Language" label="language,core" extends="model:language" maven="org.apache.camel:camel-base:3.0.0-SNAPSHOT" javaType="xxx.LanguageLanguage" description="To use the specified language in Camel expressions or predicates.">
                <property name="language" type="string" display="Language" description="The name of the language to use"/>
            </language>
        </languages>
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

    <xsl:template match="/model/processors/processor[@name='wireTap']">
        <processor name="wireTap" extends="model:processor" display="Wire Tap" label="eip,endpoint,routing" description="Routes a copy of a message (or creates a new message) to a secondary destination while continue routing the original message.">
            <property name="allowOptimisedComponents" type="boolean" display="Allow Optimised Components" description="Whether to allow components to optimise toD if they are org.apache.camel.spi.SendDynamicAware."/>
            <property name="body" type="model:expression" display="Body" kind="expression" description="Uses the expression for creating a new body as the message to use for wire tapping"/>
            <property name="cacheSize" type="int" display="Cache Size" description="Sets the maximum size used by the org.apache.camel.spi.ConsumerCache which is used to cache and reuse producers."/>
            <property name="copy" type="boolean" display="Copy" description="Uses a copy of the original exchange"/>
            <property name="dynamicUri" type="boolean" display="Dynamic Uri" description="Whether the uri is dynamic or static. If the uri is dynamic then the simple language is used to evaluate a dynamic uri to use as the wire-tap destination, for each incoming message. This works similar to how the toD EIP pattern works. If static then the uri is used as-is as the wire-tap destination."/>
            <property name="executorService" type="java:java.util.concurrent.ExecutorService" display="Executor Service" description="Uses a custom thread pool"/>
            <property name="ignoreInvalidEndpoint" type="boolean" display="Ignore Invalid Endpoint" description="Ignore the invalidate endpoint exception when try to create a producer with that endpoint"/>
            <property name="onPrepare" type="model:processor" display="On Prepare" description="Uses the Processor when preparing the org.apache.camel.Exchange to be send. This can be used to deep-clone messages that should be send, or any custom logic needed before the exchange is send."/>
            <property name="pattern" type="enum:org.apache.camel.ExchangePattern(InOnly,InOptionalOut,InOut)" display="Pattern" description="Sets the optional ExchangePattern used to invoke this endpoint"/>
            <property name="newExchange" type="model:processor" display="New Exchange" description="Processor to use for creating a new body as the message to use for wire tapping"/>
            <property name="uri" type="model:endpoint" display="Uri" required="true" description="The uri of the endpoint to send to. The uri can be dynamic computed using the org.apache.camel.language.simple.SimpleLanguage expression."/>
            <property name="headers" type="list(model:setHeader)" />
        </processor>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='wireTap']/property[@name='uri']/@type">
        <xsl:attribute name="type"><xsl:text>model:endpoint</xsl:text></xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='wireTap']/property[@name='onPrepareRef']">
        <xsl:element name="property">
            <xsl:attribute name="name">onPrepare</xsl:attribute>
            <xsl:attribute name="type">model:processor</xsl:attribute>
            <xsl:attribute name="display">On Prepare</xsl:attribute>
            <xsl:attribute name="description">Uses the {&#64;link Processor} when preparing the {&#64;link org.apache.camel.Exchange} to be send. This can be used to deep-clone messages that should be send, or any custom logic needed before the exchange is send.</xsl:attribute>
        </xsl:element>
    </xsl:template>
    <xsl:template match="/model/processors/processor[@name='wireTap']/property[@name='processorRef']">
        <xsl:element name="property">
            <xsl:attribute name="name">newExchange</xsl:attribute>
            <xsl:attribute name="type">model:processor</xsl:attribute>
            <xsl:attribute name="display">Processor</xsl:attribute>
            <xsl:attribute name="description">Sends a &lt;i&gt;new&lt;/i&gt; Exchange, instead of tapping an existing, using {&#64;link ExchangePattern#InOnly}</xsl:attribute>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/model/endpoints/endpoint[@name='spark-rest']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.component.sparkrest.SparkRestComponent</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='atmosphere-websocket']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.component.atmosphere.websocket.AtmosphereWebsocketComponent</xsl:attribute>
    </xsl:template>
    <xsl:template match="/model/endpoints/endpoint[@name='zookeeper-master']/@javaType">
        <xsl:attribute name="javaType">org.apache.camel.component.zookeepermaster.ZookeeperMasterComponent</xsl:attribute>
    </xsl:template>

    <xsl:template match="/model/endpoints/endpoint[@name='salesforce']/property[@name='operationName']/@type">
        <xsl:attribute name="type">enum:OperationName(GET_VERSIONS:getVersions,GET_RESOURCES:getResources,GET_GLOBAL_OBJECTS:getGlobalObjects,GET_BASIC_INFO:getBasicInfo,GET_DESCRIPTION:getDescription,GET_SOBJECT:getSObject,CREATE_SOBJECT:createSObject,UPDATE_SOBJECT:updateSObject,DELETE_SOBJECT:deleteSObject,GET_SOBJECT_WITH_ID:getSObjectWithId,UPSERT_SOBJECT:upsertSObject,DELETE_SOBJECT_WITH_ID:deleteSObjectWithId,GET_BLOB_FIELD:getBlobField,QUERY:query,QUERY_MORE:queryMore,QUERY_ALL:queryAll,SEARCH:search,APEX_CALL:apexCall,RECENT:recent,CREATE_JOB:createJob,GET_JOB:getJob,CLOSE_JOB:closeJob,ABORT_JOB:abortJob,CREATE_BATCH:createBatch,GET_BATCH:getBatch,GET_ALL_BATCHES:getAllBatches,GET_REQUEST:getRequest,GET_RESULTS:getResults,CREATE_BATCH_QUERY:createBatchQuery,GET_QUERY_RESULT_IDS:getQueryResultIds,GET_QUERY_RESULT:getQueryResult,GET_RECENT_REPORTS:getRecentReports,GET_REPORT_DESCRIPTION:getReportDescription,EXECUTE_SYNCREPORT:executeSyncReport,EXECUTE_ASYNCREPORT:executeAsyncReport,GET_REPORT_INSTANCES:getReportInstances,GET_REPORT_RESULTS:getReportResults,LIMITS:limits,APPROVAL:approval,APPROVALS:approvals,COMPOSITE_TREE:composite-tree,COMPOSITE_BATCH:composite-batch,COMPOSITE:composite)</xsl:attribute>
    </xsl:template>

    <xsl:template match="/model/endpoints/endpoint[@name='splunk']/property[@name='sslProtocol']/@type">
        <xsl:attribute name="type">enum:com.splunk.SSLSecurityProtocol(TLSv1_2:TLSv1.2,TLSv1_1:TLSv1.1,TLSv1:TLSv1,SSLv3:SSLv3)</xsl:attribute>
    </xsl:template>

    <xsl:template match="/model/endpoints/endpoint[@name='google-drive']/property[@name='apiName']/@type">
        <xsl:attribute name="type">enum:GoogleDriveApiName(DRIVE_ABOUT:drive-about,DRIVE_APPS:drive-apps,DRIVE_CHANGES:drive-changes,DRIVE_CHANNELS:drive-channels,DRIVE_CHILDREN:drive-children,DRIVE_COMMENTS:drive-comments,DRIVE_FILES:drive-files,DRIVE_PARENTS:drive-parents,DRIVE_PERMISSIONS:drive-permissions,DRIVE_PROPERTIES:drive-properties,DRIVE_REALTIME:drive-realtime,DRIVE_REPLIES:drive-replies,DRIVE_REVISIONS:drive-revisions)</xsl:attribute>
    </xsl:template>

    <xsl:template match="/model/verbs | /model/eips"/>

    <xsl:template match="/ | @* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
