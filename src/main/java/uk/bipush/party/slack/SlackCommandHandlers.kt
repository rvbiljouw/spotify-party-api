package uk.bipush.party.slack

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import uk.bipush.party.endpoint.*
import uk.bipush.party.model.Account
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.util.Spotify
import uk.bipush.party.util.SpotifyFilter
import java.util.concurrent.TimeUnit


object SlackCommandHandlers {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun handleListQueue(account: Account): SlackActionResponse {
        val party = account.activeParty
        if (party != null) {
            val queue = PartyQueue.forParty(party, 0, 10)
            if (queue.entries.isNotEmpty()) {
                return SlackActionResponse("in_channel", false,
                        "Current queue for ${party.name}", queue.entries.map { entry ->
                    SlackAttachment(
                            "${entry.title} - ${entry.artist ?: ""} (↑: ${entry.upvotes}, ↓: ${entry.downvotes})",
                            "Unable to display queue entry",
                            "list_queue_noop",
                            "#3AA3E3",
                            emptyList()
                    )
                })
            } else {
                return SlackActionResponse("ephemeral", false,
                        "The queue is currently empty :(")
            }
        } else {
            return SlackActionResponse("ephemeral", false,
                    "You don't have an active party")
        }
    }

    fun handleListNowPlaying(account: Account, slackCommandRequest: SlackCommandRequest): SlackActionResponse {
        val party = account.activeParty
        if (party != null) {
            val queue = PartyQueue.forParty(party, 0, 1)
            val nowPlaying = queue.nowPlaying

            if (nowPlaying != null) {
                val currentMs = System.currentTimeMillis() - nowPlaying.playedAt
                val currentPosition = String.format(
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(currentMs) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(currentMs) % TimeUnit.MINUTES.toSeconds(1)
                )

                val totalDuration = String.format(
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(nowPlaying.duration.toLong()) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(nowPlaying.duration.toLong()) % TimeUnit.MINUTES.toSeconds(1)
                )
                return SlackActionResponse("in_channel", false,
                        "Now playing in ${party.name}: " +
                                " ${nowPlaying.title} - ${nowPlaying.artist ?: ""} ($currentPosition/$totalDuration)")
            } else {
                return SlackActionResponse("ephemeral", false, "There isn't a song playing :(")
            }
        } else {
            return SlackActionResponse("ephemeral", false,
                    "You don't have an active party")
        }
    }

    fun handleListVote(account: Account, slackCommandRequest: SlackCommandRequest, upVote: Boolean): SlackActionResponse {
        val party = account.activeParty
        if (party != null) {
            val queue = PartyQueue.forParty(party, 0, 10)
            if (queue.entries.isNotEmpty()) {
                val attachment = SlackAttachment(
                        text = "Click a song to ${if (upVote) "upvote" else "downvote"} it in your current party " +
                                "(${account.activeParty?.name})",
                        fallback = "Unable to downvote song",
                        callbackId = if (upVote) "song_upvote" else "song_downvote",
                        color = "#3AA3E3",
                        actions = queue.entries.map { entry ->
                            SlackAction(
                                    text = "${entry.title} - ${entry.artist ?: ""}",
                                    name = account.id.toString(),
                                    value = entry.id.toString(),
                                    type = "button"
                            )
                        }
                )

                return SlackActionResponse("ephemeral", false,
                        "${if (upVote) "Upvote" else "Downvote"} songs",
                        listOf(attachment))
            } else {
                return SlackActionResponse("ephemeral", false, "There are no songs queued :(")
            }
        } else {
            return SlackActionResponse("ephemeral", false, "You need to have an active party to search for songs")
        }
    }

    fun handleSearchSongs(account: Account, slackCommandRequest: SlackCommandRequest): SlackActionResponse {
//        val text = slackCommandRequest.text
//        if (text == null || text.isBlank()) {
//            return SlackActionResponse("ephemeral", false, "Please enter something to search for")
//        } else {
//            if (account.activeParty != null) {
//                val split = text.split(";")
//                val song = makeFilter("TRACK", split[0])
//                val album = if (split.size > 1) makeFilter("ALBUM", split[1]) else null
//                val artist = if (split.size > 2) makeFilter("ARTIST", split[2]) else null
//
//                val tracks = Spotify.searchSongs(account.spotifyAccessToken!!, account.spotifyRefreshToken!!,
//                        listOf(song, album, artist).filterNotNull(), 0, 5)
//
//                if (tracks?.total == 0 || tracks?.items == null) {
//                    return SlackActionResponse("ephemeral", false, "Unable to find any songs")
//                } else {
//                    val attachment = SlackAttachment(
//                            text = "Click a song to queue it to your current party (${account.activeParty?.name})",
//                            fallback = "Unable to queue song",
//                            callbackId = "song_select",
//                            color = "#3AA3E3",
//                            actions = tracks.items?.map { track ->
//                                val trackArtist = if (track.artists.isNotEmpty()) track.artists[0] else null
//                                SlackAction(
//                                        text = "${track.name} - ${trackArtist?.name ?: ""}",
//                                        name = account.id.toString(),
//                                        value = mapper.writeValueAsString(
//                                                SongSelect(
//                                                        title = track.name,
//                                                        artist = trackArtist?.name ?: "",
//                                                        duration = track.duration,
//                                                        thumbnail =
//                                                        if (track.album?.images?.isNotEmpty() == true) track.album.images[0].url else "",
//                                                        uri = track.uri
//                                                )
//                                        ),
//                                        type = "button"
//                                )
//                            } ?: emptyList()
//                    )
//
//                    return SlackActionResponse("ephemeral", false,
//                            "Results for ${slackCommandRequest.userName}'s search",
//                            listOf(attachment))
//                }
//            } else {
//                return SlackActionResponse("ephemeral", false, "You need to have an active party to search for songs")
//            }
//        }
        return SlackActionResponse("ephemeral", false, "TODO - fix this")
    }

    private fun makeFilter(field: String, value: String): SpotifyFilter {
        val filter = SpotifyFilter()

        filter.type = SpotifyFilter.Type.CONTAINS
        filter.fieldName = SpotifyFilter.SpotifyField.valueOf(field.toUpperCase())
        filter.value = value
        filter.children = emptyList()

        return filter
    }
}