package ktor.sportsapi.teams

import com.google.gson.Gson
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ktor.sportsapi.constant.AppConstant
import ktor.sportsapi.constant.EndpointConstants
import ktor.sportsapi.model.*
import ktor.sportsapi.util.catchMessage
import ktor.sportsapi.util.db
import ktor.sportsapi.util.mainHttpClient
import org.litote.kmongo.eq

fun Routing.teamsRouting() {
    get("/find_leagues") {
        try {
            val collection = db().getCollection<League>()
            var leaguesObj = collection.find().toList()
            if (leaguesObj.isEmpty()) {
                leaguesObj = listLeagues
                collection.insertMany(leaguesObj)
            }
            val wrapper = BaseModel(isSuccess = true, data = leaguesObj)
            call.respond(wrapper)
        } catch (e: Exception) {
            call.application.environment.log.error("exception handled here: ${e.message}")
            val statusCode = HttpStatusCode.BadRequest
            val message = statusCode.description

            val wrapper = BaseModel<Any>(
                isSuccess = false, error = BaseErrorModel(
                    statusCode.value, ErrorType.TOAST, message = message
                )
            )

            call.respond(status = statusCode, wrapper)
        }
    }

    get("/teams_in_league") {
        try {
            val league = call.request.queryParameters["league"]
            val column = db().getCollection<Team>()
            var teamObj = column.findOne(
                Team::strLeague eq league?.replace("_", " ")
            )
            call.application.environment.log.info("find teams $teamObj")
            if (teamObj == null) {
                val response: HttpResponse =
                    mainHttpClient.get("${EndpointConstants.sportsDbUrl}${EndpointConstants.searchAllTeams}") {
                        accept(ContentType.Application.Json)
                        parameter("l", league)
                    }
                val bodyResponse = response.bodyAsText()
                val teams = Gson().fromJson(bodyResponse, Teams::class.java)
                call.application.environment.log.error("exception handled here ${response.status}")

                if (teams == null) {
                    throw ResponseException(response, AppConstant.serverBroken)
                }

                if (teams.teams == null) {
                    throw ResponseException(response, AppConstant.leagueNotFound)
                }

                teamObj = teams.teams?.get(0)
                column.insertOne(teamObj!!)
            }

            val wrapper = BaseModel(isSuccess = true, data = teamObj)

            call.respond(wrapper)
        } catch (e: Exception) {
            call.application.environment.log.error("exception handled here")
            val statusCode = HttpStatusCode.BadRequest
            var message = statusCode.description
            if (e is ResponseException) {
                message = e.catchMessage()
            }
            val wrapper = BaseModel<Any>(
                isSuccess = false, error = BaseErrorModel(
                    statusCode.value, ErrorType.TOAST, message = message
                )
            )
            call.respond(status = statusCode, wrapper)
        }
    }
}