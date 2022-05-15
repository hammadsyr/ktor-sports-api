package ktor.sportsapi.players

import com.google.gson.Gson
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import ktor.sportsapi.constant.AppConstant
import ktor.sportsapi.constant.EndpointConstants
import ktor.sportsapi.model.*
import ktor.sportsapi.util.catchMessage
import ktor.sportsapi.util.db
import ktor.sportsapi.util.mainHttpClient
import org.litote.kmongo.eq

fun Routing.playersRouting() {
    get("/search_player") {
        try {
            val name = call.request.queryParameters["name"]
            val column = db().getCollection<Player>()
            var playerObj = column.findOne(
                Player::strPlayer eq name?.replace("_", " "),
                Player::strPlayer eq name?.replace("_", " ")?.toLowerCasePreservingASCIIRules()
            )

            call.application.environment.log.info(playerObj.toString())
            if (playerObj == null) {
                val response: HttpResponse =
                    mainHttpClient.get("${EndpointConstants.sportsDbUrl}${EndpointConstants.searchPlayers}") {
                        accept(ContentType.Application.Json)
                        parameter("p", name)
                    }
                val bodyResponse = response.bodyAsText()
                val details = Gson().fromJson(bodyResponse, PlayerDetails::class.java)
                call.application.environment.log.error("exception handled here ${response.status}")

                if (details == null) {
                    throw ResponseException(response, AppConstant.serverBroken)
                }

                if (details.player == null) {
                    throw ResponseException(response, AppConstant.playerNotFound)
                }
                playerObj = details.player?.get(0)
                column.insertOne(playerObj!!)
            }

            val wrapper = BaseModel(isSuccess = true, data = playerObj)

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