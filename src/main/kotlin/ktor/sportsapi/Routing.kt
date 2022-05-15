package ktor.sportsapi

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ktor.sportsapi.auth.User
import ktor.sportsapi.auth.authRouting
import ktor.sportsapi.favorites.favoritesRouting
import ktor.sportsapi.model.BaseErrorModel
import ktor.sportsapi.model.BaseModel
import ktor.sportsapi.model.ErrorType
import ktor.sportsapi.players.playersRouting
import ktor.sportsapi.teams.teamsRouting
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome!!!")
        }

        teamsRouting()
        authRouting()
        playersRouting()
        favoritesRouting()
    }
}
