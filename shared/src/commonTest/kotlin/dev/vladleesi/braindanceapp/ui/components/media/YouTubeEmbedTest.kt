// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YouTubeEmbedTest {
    private val video = GameMedia.YouTubeVideo("yowv6_rspoM")

    @Test
    fun theWebEmbedIsThePrivacyEnhancedPlayerTalkingOnlyToTheAppsOrigin() {
        assertEquals(
            "https://www.youtube-nocookie.com/embed/yowv6_rspoM?autoplay=1&rel=0&playsinline=1&enablejsapi=1" +
                "&origin=https%3A%2F%2Fapp.braindancegg.com",
            video.webEmbedUrl("https://app.braindancegg.com"),
        )
    }

    @Test
    fun embedMessagesDistinguishPausedVideosFromUnstartedOrEndedVideos() {
        assertEquals(EmbedEvent.READY, embedEventOf("""{"event":"onReady","id":1}"""))
        assertEquals(EmbedEvent.AUTOPLAY_BLOCKED, embedEventOf("""{"event":"onAutoplayBlocked","id":1}"""))
        assertEquals(EmbedEvent.FAILED, embedEventOf("""{"event":"onError","info":101}"""))
        assertEquals(EmbedEvent.PLAYING, embedEventOf("""{"event":"onStateChange","info":1}"""))
        assertEquals(EmbedEvent.BUFFERING, embedEventOf("""{"event":"infoDelivery","info":{"playerState":3}}"""))
        assertEquals(EmbedEvent.PAUSED, embedEventOf("""{"event":"onStateChange","info":2}"""))
        assertEquals(EmbedEvent.PAUSED, embedEventOf("""{"event":"infoDelivery","info":{"playerState":2}}"""))
        assertEquals(EmbedEvent.STOPPED, embedEventOf("""{"event":"onStateChange","info":0}"""))
        assertEquals(EmbedEvent.STOPPED, embedEventOf("""{"event":"initialDelivery","info":{"playerState":-1}}"""))
    }

    @Test
    fun otherEmbedMessagesAreIgnored() {
        assertEquals(null, embedEventOf("""{"event":"infoDelivery","info":{"currentTime":12.5}}"""))
        assertEquals(null, embedEventOf("""{"event":"onStateChange","info":"playing"}"""))
        assertEquals(null, embedEventOf("""["onReady"]"""))
        assertEquals(null, embedEventOf("not json"))
        assertEquals(null, embedEventOf(""))
    }

    @Test
    fun embedCommandsCallThePlayersFunction() {
        assertEquals(
            """{"event":"command","func":"pauseVideo","args":[],"id":1,"channel":"widget"}""",
            embedCommand("pauseVideo"),
        )
        assertEquals(
            """{"event":"command","func":"addEventListener","args":["onStateChange"],"id":1,"channel":"widget"}""",
            embedCommand("addEventListener", "onStateChange"),
        )
    }

    @Test
    fun thePlayerPageLoadsOnlyYouTubesApiAndPrivacyEnhancedPlayer() {
        val page = video.playerPageHtml()

        assertTrue("videoId: 'yowv6_rspoM'" in page)
        assertTrue("host: 'https://www.youtube-nocookie.com'" in page)
        assertTrue(
            "default-src 'none'; script-src 'unsafe-inline' https://www.youtube.com; " +
                "frame-src https://www.youtube-nocookie.com" in page,
        )
        // YouTube's own fullscreen needs native support the web views lack.
        assertTrue("fs: 0" in page)
        val hosts = Regex("""https://[a-z0-9.-]+""").findAll(page).map { it.value }.toSet()
        assertEquals(setOf("https://www.youtube.com", "https://www.youtube-nocookie.com"), hosts)
    }

    @Test
    fun thePlayerPageSignalsAFailureOnceThroughItsErrorAddress() {
        val page = video.playerPageHtml()

        assertTrue("onError: braindanceFail" in page, "a video YouTube cannot play here")
        assertTrue(
            """src="https://www.youtube.com/iframe_api" onerror="braindanceFail()"""" in page,
            "YouTube unreachable",
        )
        assertTrue("window.location.href = 'braindance-player:error'" in page)
        assertTrue("if (braindanceFailed) return;" in page, "reported once")
        // The app reads nothing back from the page: no polling hook and no bridge to call.
        assertFalse("webkit.messageHandlers" in page || "postMessage" in page)
    }
}
