<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dyn="http://exslt.org/dynamic" version="1.0"
                extension-element-prefixes="dyn">
    <xsl:output indent="yes" method="xml"/>

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
