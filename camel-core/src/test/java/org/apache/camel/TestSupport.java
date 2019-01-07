/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.ErrorHandlerSupport;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.PredicateAssertHelper;
import org.apache.camel.util.IOHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bunch of useful testing methods
 */
public abstract class TestSupport extends Assert {

    protected static final String LS = System.lineSeparator();
    private static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);

    @Rule
    public TestName name = new TestName();

    @Rule
    public RetryRule rule = new RetryRule(3);

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected Path testDataDir;

    @Override
    public String toString() {
        return getName() + "(" + getClass().getName() + ")";
    }

    public String getName() {
        return name.getMethodName();
    }

    @Before
    public void setUp() throws Exception {
        //start with a clean slate
        DefaultCamelContext.setContextCounter(0);
        TestSupportNodeIdFactory.resetCounters();
        Assume.assumeTrue(canRunOnThisPlatform());
    }

    @After
    public void tearDown() throws Exception {
        // make sure we cleanup the platform mbean server
        TestSupportJmxCleanup.removeMBeans(null);
        // forget data dir
        testDataDir = null;
    }

    protected Path getCurrentDir() {
        return Paths.get(".");
    }

    protected Path getBaseDir() {
        String basedir = System.getProperty("baseDirectory", ".");
        return Paths.get(basedir);
    }

    protected Path getBuildDir() {
        String builddir = System.getProperty("buildDirectory", "target");
        return getBaseDir().resolve(builddir);
    }

    protected Path getDataDir() {
        String datadir = System.getProperty("dataDirectory", "data");
        return getBuildDir().resolve(datadir);
    }

    protected Path getTestDataDir(String dir) {
        return getDataDir().resolve(dir);
    }

    protected Path getTestDataDir() {
        synchronized (this) {
            if (testDataDir == null) {
                String testdatadir = System.getProperty("testDataDirectory", "data");
                Path parentPath = getBuildDir().resolve(testdatadir);
                testDataDir = parentPath.resolve(getClass().getSimpleName().toLowerCase());
                try {
                    deleteDirectory(testDataDir);
                    Files.createDirectories(testDataDir);
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }
            return testDataDir;
        }
    }

    protected boolean canRunOnThisPlatform() {
        return true;
    }

    // Builder methods for expressions used when testing
    // -------------------------------------------------------------------------

    /**
     * Returns a value builder for the given header
     */
    public static ValueBuilder header(String name) {
        return Builder.header(name);
    }

    /**
     * Returns a value builder for the given exchange property
     */
    public static ValueBuilder exchangeProperty(String name) {
        return Builder.exchangeProperty(name);
    }

    /**
     * Returns a predicate and value builder for the inbound body on an exchange
     */
    public static ValueBuilder body() {
        return Builder.body();
    }

    /**
     * Returns a predicate and value builder for the inbound message body as a
     * specific type
     */
    public static <T> ValueBuilder bodyAs(Class<T> type) {
        return Builder.bodyAs(type);
    }

    /**
     * Returns a value builder for the given system property
     */
    public static ValueBuilder systemProperty(String name) {
        return Builder.systemProperty(name);
    }

    /**
     * Returns a value builder for the given system property
     */
    public static ValueBuilder systemProperty(String name, String defaultValue) {
        return Builder.systemProperty(name, defaultValue);
    }

    // Assertions
    // -----------------------------------------------------------------------

    public static <T> T assertIsInstanceOf(Class<T> expectedType, Object value) {
        assertNotNull("Expected an instance of type: " + expectedType.getName() + " but was null", value);
        assertTrue("object should be a " + expectedType.getName() + " but was: " + value + " with type: "
                + value.getClass().getName(), expectedType.isInstance(value));
        return expectedType.cast(value);
    }

    public static void assertEndpointUri(Endpoint endpoint, String uri) {
        assertNotNull("Endpoint is null when expecting endpoint for: " + uri, endpoint);
        assertEquals("Endoint uri for: " + endpoint, uri, endpoint.getEndpointUri());
    }

    /**
     * Asserts the In message on the exchange contains the expected value
     */
    public static Object assertInMessageHeader(Exchange exchange, String name, Object expected) {
        return assertMessageHeader(exchange.getIn(), name, expected);
    }

    /**
     * Asserts the Out message on the exchange contains the expected value
     */
    public static Object assertOutMessageHeader(Exchange exchange, String name, Object expected) {
        return assertMessageHeader(exchange.getOut(), name, expected);
    }

    /**
     * Asserts that the given exchange has an OUT message of the given body value
     *
     * @param exchange the exchange which should have an OUT message
     * @param expected the expected value of the OUT message
     * @throws InvalidPayloadException is thrown if the payload is not the expected class type
     */
    public static void assertInMessageBodyEquals(Exchange exchange, Object expected) throws InvalidPayloadException {
        assertNotNull("Should have a response exchange!", exchange);

        Object actual;
        if (expected == null) {
            actual = exchange.getIn().getMandatoryBody();
            assertEquals("in body of: " + exchange, expected, actual);
        } else {
            actual = exchange.getIn().getMandatoryBody(expected.getClass());
        }
        assertEquals("in body of: " + exchange, expected, actual);

        LOG.debug("Received response: {} with in: {}", exchange, exchange.getIn());
    }

    /**
     * Asserts that the given exchange has an OUT message of the given body value
     *
     * @param exchange the exchange which should have an OUT message
     * @param expected the expected value of the OUT message
     * @throws InvalidPayloadException is thrown if the payload is not the expected class type
     */
    public static void assertOutMessageBodyEquals(Exchange exchange, Object expected) throws InvalidPayloadException {
        assertNotNull("Should have a response exchange!", exchange);

        Object actual;
        if (expected == null) {
            actual = exchange.getOut().getMandatoryBody();
            assertEquals("output body of: " + exchange, expected, actual);
        } else {
            actual = exchange.getOut().getMandatoryBody(expected.getClass());
        }
        assertEquals("output body of: " + exchange, expected, actual);

        LOG.debug("Received response: {} with out: {}", exchange, exchange.getOut());
    }

    public static Object assertMessageHeader(Message message, String name, Object expected) {
        Object value = message.getHeader(name);
        assertEquals("Header: " + name + " on Message: " + message, expected, value);
        return value;
    }

    public static Object assertProperty(Exchange exchange, String name, Object expected) {
        Object value = exchange.getProperty(name);
        assertEquals("Property: " + name + " on Exchange: " + exchange, expected, value);
        return value;
    }

    /**
     * Asserts that the given expression when evaluated returns the given answer
     */
    public static Object assertExpression(Expression expression, Exchange exchange, Object expected) {
        Object value;
        if (expected != null) {
            value = expression.evaluate(exchange, expected.getClass());
        } else {
            value = expression.evaluate(exchange, Object.class);
        }

        LOG.debug("Evaluated expression: {} on exchange: {} result: {}", expression, exchange, value);

        assertEquals("Expression: " + expression + " on Exchange: " + exchange, expected, value);
        return value;
    }

    /**
     * Asserts that the predicate returns the expected value on the exchange
     */
    public static void assertPredicateMatches(Predicate predicate, Exchange exchange) {
        assertPredicate(predicate, exchange, true);
    }

    /**
     * Asserts that the predicate returns the expected value on the exchange
     */
    public static void assertPredicateDoesNotMatch(Predicate predicate, Exchange exchange) {
        try {
            PredicateAssertHelper.assertMatches(predicate, "Predicate should match: ", exchange);
        } catch (AssertionError e) {
            LOG.debug("Caught expected assertion error: " + e);
        }
        assertPredicate(predicate, exchange, false);
    }

    /**
     * Asserts that the predicate returns the expected value on the exchange
     */
    public static boolean assertPredicate(final Predicate predicate, Exchange exchange, boolean expected) {
        if (expected) {
            PredicateAssertHelper.assertMatches(predicate, "Predicate failed: ", exchange);
        }
        boolean value = predicate.matches(exchange);

        LOG.debug("Evaluated predicate: {} on exchange: {} result: {}", predicate, exchange, value);

        assertEquals("Predicate: " + predicate + " on Exchange: " + exchange, expected, value);
        return value;
    }

    /**
     * Resolves an endpoint and asserts that it is found
     */
    public static Endpoint resolveMandatoryEndpoint(CamelContext context, String uri) {
        Endpoint endpoint = context.getEndpoint(uri);

        assertNotNull("No endpoint found for URI: " + uri, endpoint);

        return endpoint;
    }

    /**
     * Resolves an endpoint and asserts that it is found
     */
    public static <T extends Endpoint> T resolveMandatoryEndpoint(CamelContext context, String uri,
                                                                  Class<T> endpointType) {
        T endpoint = context.getEndpoint(uri, endpointType);

        assertNotNull("No endpoint found for URI: " + uri, endpoint);

        return endpoint;
    }

    /**
     * Creates an exchange with the given body
     */
    protected Exchange createExchangeWithBody(CamelContext camelContext, Object body) {
        Exchange exchange = new DefaultExchange(camelContext);
        Message message = exchange.getIn();
        message.setHeader("testName", getName());
        message.setHeader("testClass", getClass().getName());
        message.setBody(body);
        return exchange;
    }

    public static <T> T assertOneElement(List<T> list) {
        assertEquals("Size of list should be 1: " + list, 1, list.size());
        return list.get(0);
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> List<T> assertListSize(List<T> list, int size) {
        return assertListSize("List", list, size);
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> List<T> assertListSize(String message, List<T> list, int size) {
        assertEquals(message + " should be of size: "
                + size + " but is: " + list, size, list.size());
        return list;
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> Collection<T> assertCollectionSize(Collection<T> list, int size) {
        return assertCollectionSize("List", list, size);
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> Collection<T> assertCollectionSize(String message, Collection<T> list, int size) {
        assertEquals(message + " should be of size: "
                + size + " but is: " + list, size, list.size());
        return list;
    }

    /**
     * A helper method to create a list of Route objects for a given route builder
     */
    public static List<Route> getRouteList(RouteBuilder builder) throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(builder);
        context.start();
        List<Route> answer = context.getRoutes();
        context.stop();
        return answer;
    }

    /**
     * Asserts that the text contains the given string
     *
     * @param text          the text to compare
     * @param containedText the text which must be contained inside the other text parameter
     */
    public static void assertStringContains(String text, String containedText) {
        assertNotNull("Text should not be null!", text);
        assertTrue("Text: " + text + " does not contain: " + containedText, text.contains(containedText));
    }

    /**
     * If a processor is wrapped with a bunch of DelegateProcessor or DelegateAsyncProcessor objects
     * this call will drill through them and return the wrapped Processor.
     */
    public static Processor unwrap(Processor processor) {
        while (true) {
            if (processor instanceof DelegateProcessor) {
                processor = ((DelegateProcessor) processor).getProcessor();
            } else {
                return processor;
            }
        }
    }

    /**
     * If a processor is wrapped with a bunch of DelegateProcessor or DelegateAsyncProcessor objects
     * this call will drill through them and return the Channel.
     * <p/>
     * Returns null if no channel is found.
     */
    public static Channel unwrapChannel(Processor processor) {
        while (true) {
            if (processor instanceof Channel) {
                return (Channel) processor;
            } else if (processor instanceof DelegateProcessor) {
                processor = ((DelegateProcessor) processor).getProcessor();
            } else if (processor instanceof ErrorHandlerSupport) {
                processor = ((ErrorHandlerSupport) processor).getOutput();
            } else {
                return null;
            }
        }
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param file the directory to be deleted
     */
    public static void deleteDirectory(String file) {
        deleteDirectory(new File(file));
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param file the directory to be deleted
     */
    public static void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File child : files) {
                deleteDirectory(child);
            }
        }

        file.delete();
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param path the directory to be deleted
     */
    public static void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * create the directory
     *
     * @param file the directory to be created
     */
    public static void createDirectory(String file) {
        File dir = new File(file);
        dir.mkdirs();
    }

    /**
     * create the directory
     *
     * @param path the directory to be created
     */
    public static void createDirectory(Path path) throws IOException {
        Files.createDirectories(path);
    }

    /**
     * To be used for folder/directory comparison that works across different platforms such
     * as Window, Mac and Linux.
     */
    public static void assertDirectoryEquals(String expected, String actual) {
        assertDirectoryEquals(null, expected, actual);
    }

    /**
     * To be used for folder/directory comparison that works across different platforms such
     * as Window, Mac and Linux.
     */
    public static void assertDirectoryEquals(String message, String expected, String actual) {
        // must use single / as path separators
        String expectedPath = expected.replace('\\', '/');
        String actualPath = actual.replace('\\', '/');

        if (message != null) {
            assertEquals(message, expectedPath, actualPath);
        } else {
            assertEquals(expectedPath, actualPath);
        }
    }

    /**
     * To be used to check is a file is found in the file system
     */
    public static void assertFileExists(String filename) {
        File file = new File(filename);
        assertTrue("File " + filename + " should exist", file.exists());
    }

    /**
     * To be used to check is a file is <b>not</b> found in the file system
     */
    public static void assertFileNotExists(String filename) {
        File file = new File(filename);
        assertFalse("File " + filename + " should not exist", file.exists());
    }

    /**
     * Is this OS the given platform.
     * <p/>
     * Uses <tt>os.name</tt> from the system properties to determine the OS.
     *
     * @param platform such as Windows
     * @return <tt>true</tt> if its that platform.
     */
    public static boolean isPlatform(String platform) {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        return osName.contains(platform.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Is this Java by the given vendor.
     * <p/>
     * Uses <tt>java.vendor</tt> from the system properties to determine the vendor.
     *
     * @param vendor such as IBM
     * @return <tt>true</tt> if its that vendor.
     */
    public static boolean isJavaVendor(String vendor) {
        String javaVendor = System.getProperty("java.vendor").toLowerCase(Locale.ENGLISH);
        return javaVendor.contains(vendor.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Is this version the given Java version.
     * <p/>
     * Uses <tt>java.version</tt> from the system properties to determine the version.
     *
     * @param version such as 1.6 or 6
     * @return <tt>true</tt> if its that vendor.
     */
    public static boolean isJavaVersion(String version) {
        if (version.contains(".")) { //before jdk 9
            return Integer.parseInt(version.split("\\.")[1]) == getJavaMajorVersion();
        } else {
            return Integer.parseInt(version) == getJavaMajorVersion();
        }
    }

    /**
     * Returns the current major Java version e.g 8.
     * <p/>
     * Uses <tt>java.specification.version</tt> from the system properties to determine the major version.

     * @return the current major Java version.
     */
    public static int getJavaMajorVersion() {
        String javaSpecVersion = System.getProperty("java.specification.version");
        if (javaSpecVersion.contains(".")) { //before jdk 9
            return Integer.parseInt(javaSpecVersion.split("\\.")[1]);
        } else {
            return Integer.parseInt(javaSpecVersion);
        }
    }

    /**
     * Used for registering a sysetem property.
     * <p/>
     * if the property already contains the passed value nothing will happen.
     * If the system property has already a value, the passed value will be appended separated by <tt>separator</tt>
     *
     * @param sysPropertyName   the name of the system property to be set
     * @param sysPropertyValue  the value to be set for the system property passed as sysPropertyName
     * @param separator         the property separator to be used to append sysPropertyValue
     *
     */
    public static void registerSystemProperty(String sysPropertyName, String sysPropertyValue, String separator) {
        synchronized (System.getProperties()) {
            if (System.getProperties().contains(sysPropertyName)) {
                String current = System.getProperty(sysPropertyName);
                if (!current.contains(sysPropertyValue)) {
                    System.setProperty(sysPropertyName, current + separator + sysPropertyValue);
                }
            } else {
                System.setProperty(sysPropertyName, sysPropertyValue);
            }
        }
    }

    public static class RetryRule implements TestRule {

        private AtomicInteger retryCount;

        public RetryRule(int retries){
            super();
            this.retryCount = new AtomicInteger(retries);
        }
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (true || description.getAnnotation(Retry.class) != null) {
                        Throwable caughtThrowable = null;
                        while (retryCount.getAndDecrement() > 0) {
                            try {
                                base.evaluate();
                                return;
                            } catch (AssumptionViolatedException e) {
                                throw e;
                            } catch (Throwable t) {
                                if (caughtThrowable == null) {
                                    caughtThrowable = new RuntimeException(description.getDisplayName() + ": Failed");
                                }
                                caughtThrowable.addSuppressed(t);
                                if (retryCount.get() > 0) {
                                    System.err.println(
                                            description.getDisplayName() +
                                                    ": Failed, " +
                                                    retryCount.toString() +
                                                    "retries remain");
                                } else {
                                    throw caughtThrowable;
                                }
                            }
                        }
                    } else {
                        base.evaluate();
                    }
                }
            };
        }
    }

    //Retry.java
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Retry {}

}
