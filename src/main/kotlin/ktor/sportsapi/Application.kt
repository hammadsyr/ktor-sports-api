package ktor.sportsapi

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ktor.sportsapi.auth.User
import ktor.sportsapi.model.BaseErrorModel
import ktor.sportsapi.model.BaseModel
import ktor.sportsapi.model.ErrorType
import ktor.sportsapi.util.appErrorWrapper
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.mainModule() {
    install(ContentNegotiation) {
        gson {

        }
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            appErrorWrapper(call, status)
        }

        status(HttpStatusCode.InternalServerError) { call, status ->
            appErrorWrapper(call, status)
        }
    }

    configureRouting()
}

