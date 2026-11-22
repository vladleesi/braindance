// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import dev.vladleesi.braindanceapp.navigation.APP_LINK_ORIGIN
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/*
 * YouTube's privacy-enhanced player, as the gallery's preview embeds it. Every address and page here is built from a
 * video ID that toYouTubeIdOrNull validated, so nothing from the API is interpolated into a URL, HTML, or script.
 */

/** The origin of YouTube's privacy-enhanced player; the only frame the app embeds. */
internal const val YOUTUBE_EMBED_ORIGIN = "https://www.youtube-nocookie.com"

/**
 * The address Android's and iOS's web views load the player page as. YouTube requires an embedding page to identify
 * itself; the page is the app's own, with no content from that address.
 */
internal const val PLAYER_PAGE_BASE_URL = "$APP_LINK_ORIGIN/"

/**
 * The address the player page navigates to when the video cannot play, such as when its owner does not allow
 * embedding or YouTube cannot be reached. The web views intercept it instead of loading it, so the page signals the
 * failure once, without a JavaScript interface or polling.
 */
internal const val PLAYER_ERROR_URL = "braindance-player:error"

/**
 * The embed address for the Web, for a page at [pageOrigin]. It plays once loaded, which happens only after the user
 * presses play. YouTube's player shows its own message, with a link to YouTube, for a video it cannot play. It opens
 * YouTube's message channel to [pageOrigin] alone, through which the page learns whether the video plays and asks it
 * to play or pause (see [EmbedEvent] and [embedCommand]).
 */
internal fun GameMedia.YouTubeVideo.webEmbedUrl(pageOrigin: String): String =
    "$YOUTUBE_EMBED_ORIGIN/embed/$youTubeId?autoplay=1&rel=0&playsinline=1&enablejsapi=1" +
        "&origin=${pageOrigin.encodeURLParameter()}"

/** What the Web page learns from YouTube's embedded player through its message channel. */
internal enum class EmbedEvent {
    /** The player can take commands. */
    READY,

    /** The browser declined scripted playback. */
    AUTOPLAY_BLOCKED,

    /** YouTube reported a playback error. */
    FAILED,

    /** The video plays. */
    PLAYING,

    /** The video is paused and can resume from the same position. */
    PAUSED,

    /** The video loads to play, before it starts or when playback waits for data. */
    BUFFERING,

    /** The video ended or has not started. */
    STOPPED,
}

private const val STATE_PLAYING = 1
private const val STATE_PAUSED = 2
private const val STATE_BUFFERING = 3

/**
 * The event in [message], a message the embedded player posted, or null for any other message, including one that is
 * not JSON. Only the event's name and the player's state are read.
 */
internal fun embedEventOf(message: String): EmbedEvent? {
    val json =
        try {
            Json.parseToJsonElement(message) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return null
    val info = json["info"]
    val state =
        when ((json["event"] as? JsonPrimitive)?.contentOrNull) {
            "onReady" -> return EmbedEvent.READY
            "onAutoplayBlocked" -> return EmbedEvent.AUTOPLAY_BLOCKED
            "onError" -> return EmbedEvent.FAILED
            "onStateChange" -> (info as? JsonPrimitive)?.intOrNull
            "initialDelivery", "infoDelivery" ->
                (
                    (info as? JsonObject)?.get(
                        "playerState",
                    ) as? JsonPrimitive
                )?.intOrNull
            else -> null
        } ?: return null
    return when (state) {
        STATE_PLAYING -> EmbedEvent.PLAYING
        STATE_PAUSED -> EmbedEvent.PAUSED
        STATE_BUFFERING -> EmbedEvent.BUFFERING
        else -> EmbedEvent.STOPPED
    }
}

/** The message that subscribes the page to the embedded player's events; the player may need it more than once. */
internal const val EMBED_LISTENING_MESSAGE = """{"event":"listening","id":1,"channel":"widget"}"""

/**
 * The message that calls the embedded player's [function], such as `playVideo` or `pauseVideo`, with [argument] if
 * it takes one. Both are the app's own constants, never input.
 */
internal fun embedCommand(
    function: String,
    argument: String? = null,
): String {
    val args = argument?.let { "\"$it\"" } ?: ""
    return """{"event":"command","func":"$function","args":[$args],"id":1,"channel":"widget"}"""
}

/**
 * The page Android's and iOS's web views load for this video. It uses YouTube's IFrame Player API with the
 * privacy-enhanced host, plays at once (the user pressed play to create it), and keeps YouTube's own fullscreen button
 * off, which needs native support the web views lack. Its policy allows only YouTube's API script and player frame.
 * A failure navigates to [PLAYER_ERROR_URL].
 */
internal fun GameMedia.YouTubeVideo.playerPageHtml(): String =
    """
    <!DOCTYPE html>
    <html>
    <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline' https://www.youtube.com; frame-src $YOUTUBE_EMBED_ORIGIN; style-src 'unsafe-inline'">
    <style>html, body, #player { margin: 0; width: 100%; height: 100%; overflow: hidden; background: #000; }</style>
    </head>
    <body>
    <div id="player"></div>
    <script>
    var braindanceFailed = false;
    function braindanceFail() {
      if (braindanceFailed) return;
      braindanceFailed = true;
      window.location.href = '$PLAYER_ERROR_URL';
    }
    function onYouTubeIframeAPIReady() {
      new YT.Player('player', {
        host: '$YOUTUBE_EMBED_ORIGIN',
        videoId: '$youTubeId',
        width: '100%',
        height: '100%',
        playerVars: { autoplay: 1, playsinline: 1, rel: 0, fs: 0 },
        events: {
          onReady: function (event) { event.target.playVideo(); },
          onError: braindanceFail
        }
      });
    }
    </script>
    <script src="https://www.youtube.com/iframe_api" onerror="braindanceFail()"></script>
    </body>
    </html>
    """.trimIndent()
