package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class SessionTrackerPauseResumeTest {

    private val configuration = generateConfiguration().also {
        it.autoTrackSessions = false
    }
    private lateinit var tracker: SessionTracker

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var activityManager: ActivityManager

    @Mock
    lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        `when`(client.getAppContext()).thenReturn(context)
        `when`(context.getSystemService("activity")).thenReturn(activityManager)
        tracker = SessionTracker(
            BugsnagTestUtils.generateImmutableConfig(),
            configuration.callbackState, client, sessionStore, NoopLogger
        )
    }

    /**
     * Verifies that a session can be resumed after it is paused
     */
    @Test
    fun resumeFromPausedSession() {
        tracker.startSession(false)
        val originalSession = tracker.currentSession
        assertNotNull(originalSession)

        tracker.pauseSession()
        assertNull(tracker.currentSession)

        assertTrue(tracker.resumeSession())
        assertEquals(originalSession, tracker.currentSession)
    }

    /**
     * Verifies that a new session is started when calling [SessionTracker.resumeSession],
     * if there is no paused session
     */
    @Test
    fun resumeWithNoPausedSession() {
        assertNull(tracker.currentSession)
        assertFalse(tracker.resumeSession())
        assertNotNull(tracker.currentSession)
    }

    /**
     * Verifies that a new session can be created after the previous one is paused
     */
    @Test
    fun startNewAfterPausedSession() {
        tracker.startSession(false)
        val originalSession = tracker.currentSession

        tracker.pauseSession()
        tracker.startSession(false)
        assertNotEquals(originalSession, tracker.currentSession)
    }

    /**
     * Verifies that calling [SessionTracker.resumeSession] multiple times only starts one session
     */
    @Test
    fun multipleResumesHaveNoEffect() {
        tracker.startSession(false)
        val original = tracker.currentSession
        tracker.pauseSession()

        assertTrue(tracker.resumeSession())
        assertEquals(original, tracker.currentSession)

        assertFalse(tracker.resumeSession())
        assertEquals(original, tracker.currentSession)
    }

    /**
     * Verifies that calling [SessionTracker.pauseSession] multiple times only pauses one session
     */
    @Test
    fun multiplePausesHaveNoEffect() {
        tracker.startSession(false)
        assertNotNull(tracker.currentSession)

        tracker.pauseSession()
        assertNull(tracker.currentSession)

        tracker.pauseSession()
        assertNull(tracker.currentSession)
    }

    /**
     * Verifies that if a handled or unhandled error occurs when a session is paused, the
     * error count is not updated
     */
    @Test
    fun pausedSessionDoesNotIncrement() {
        tracker.startSession(false)
        tracker.incrementHandledAndCopy()
        tracker.incrementUnhandledAndCopy()
        assertEquals(1, tracker.currentSession?.handledCount)
        assertEquals(1, tracker.currentSession?.unhandledCount)

        tracker.pauseSession()
        tracker.incrementHandledAndCopy()
        tracker.incrementUnhandledAndCopy()
        tracker.resumeSession()
        assertEquals(1, tracker.currentSession?.handledCount)
        assertEquals(1, tracker.currentSession?.unhandledCount)

        tracker.incrementHandledAndCopy()
        tracker.incrementUnhandledAndCopy()
        assertEquals(2, tracker.currentSession?.handledCount)
        assertEquals(2, tracker.currentSession?.unhandledCount)
    }
}