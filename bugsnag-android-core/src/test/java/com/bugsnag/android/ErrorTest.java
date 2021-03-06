package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ErrorTest {

    private Configuration config;
    private Error error;

    /**
     * Generates a new default error for use by tests
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
        RuntimeException exception = new RuntimeException("Example message");
        error = new Error.Builder(config, exception, null,
            Thread.currentThread(), false).build();
    }

    @Test
    public void testShouldIgnoreClass() {
        config.setIgnoreClasses(new String[]{"java.io.IOException"});

        // Shouldn't ignore classes not in ignoreClasses
        RuntimeException runtimeException = new RuntimeException("Test");
        Error error = new Error.Builder(config,
            runtimeException, null, Thread.currentThread(), false).build();
        assertFalse(error.shouldIgnoreClass());

        // Should ignore errors in ignoreClasses
        IOException ioException = new IOException("Test");
        error = new Error.Builder(config,
            ioException, null, Thread.currentThread(), false).build();
        assertTrue(error.shouldIgnoreClass());
    }

    @Test
    public void checkExceptionMessageNullity() throws Exception {
        String msg = "Foo";
        Error err = new Error.Builder(config,
            new RuntimeException(msg), null,
            Thread.currentThread(), false).build();
        assertEquals(msg, err.getExceptionMessage());

        err = new Error.Builder(config,
            new RuntimeException(), null,
            Thread.currentThread(), false).build();
        assertEquals("", err.getExceptionMessage());
    }

    @Test
    public void testNullSeverity() throws Exception {
        error.setSeverity(null);
        assertEquals(Severity.WARNING, error.getSeverity());
    }

    @Test
    public void testBugsnagExceptionName() throws Exception {
        BugsnagException exception = new BugsnagException("Busgang", "exceptional",
            new StackTraceElement[]{});
        Error err = new Error.Builder(config,
            exception, null, Thread.currentThread(), false).build();
        assertEquals("Busgang", err.getExceptionName());
    }

    @Test
    public void testNullContext() throws Exception {
        error.setContext(null);
        error.setAppData(null);
        assertNull(error.getContext());
    }

    @Test
    public void testSetUser() throws Exception {
        String firstId = "123";
        String firstEmail = "fake@example.com";
        String firstName = "Bob Swaggins";
        error.setUser(firstId, firstEmail, firstName);

        assertEquals(firstId, error.getUser().getId());
        assertEquals(firstEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userId = "foo";
        error.setUserId(userId);
        assertEquals(userId, error.getUser().getId());
        assertEquals(firstEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userEmail = "another@example.com";
        error.setUserEmail(userEmail);
        assertEquals(userId, error.getUser().getId());
        assertEquals(userEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userName = "Isaac";
        error.setUserName(userName);
        assertEquals(userId, error.getUser().getId());
        assertEquals(userEmail, error.getUser().getEmail());
        assertEquals(userName, error.getUser().getName());
    }

    @Test
    public void testBuilderMetaData() {
        Configuration config = new Configuration("api-key");
        Error.Builder builder = new Error.Builder(config,
            new RuntimeException("foo"), null,
            Thread.currentThread(), false);

        assertNotNull(builder.metaData(new MetaData()).build());

        MetaData metaData = new MetaData();
        metaData.addToTab("foo", "bar", true);

        Error error = builder.metaData(metaData).build();
        assertEquals(1, error.getMetaData().getTab("foo").size());
    }

    @Test
    public void testErrorMetaData() {
        error.addToTab("rocks", "geode", "a shiny mineral");
        assertNotNull(error.getMetaData().getTab("rocks"));

        error.clearTab("rocks");
        assertTrue(error.getMetaData().getTab("rocks").isEmpty());
    }
}
