package ktor.sportsapi.teams

import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class League(
    @BsonId val id: Id<Int> = newId(), val idLeague: String, val strLeague: String
)

val listLeagues = listOf(
    League(idLeague = "4328", strLeague = "English Premier League"),
    League(idLeague = "4331", strLeague = "German Bundesliga"),
    League(idLeague = "4332", strLeague = "Italian Serie A"),
    League(idLeague = "4335", strLeague = "Spanish La Liga"),
    League(idLeague = "4337", strLeague = "Dutch Eredivisie"),
)