package ktor.sportsapi.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.JsonObject
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ktor.sportsapi.model.BaseErrorModel
import ktor.sportsapi.model.BaseModel
import ktor.sportsapi.model.ErrorType
import ktor.sportsapi.util.catchMessage
import ktor.sportsapi.util.db
import ktor.sportsapi.util.hashHMAC
import org.litote.kmongo.eq
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

fun Route.authRouting() {
    var jwkProvider: JwkProvider
    var privateKeyString: String
    var issuer: String
    var audience: String

    application.apply {
        privateKeyString = environment.config.property("jwt.privateKey").getString()
        issuer = environment.config.property("jwt.issuer").getString()
        audience = environment.config.property("jwt.audience").getString()
        val myRealm = environment.config.property("jwt.realm").getString()
        jwkProvider = JwkProviderBuilder(issuer)
            .cached(10, 1, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.HOURS)
            .build()

        install(Authentication) {
            jwt("auth-jwt") {
                realm = myRealm
                verifier(jwkProvider, issuer) {
                    acceptLeeway(3)
                }
                validate { credential ->
                    if (credential.payload.getClaim("username").asString() != "") {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
                challenge { defaultScheme, realm ->
                    val errorModel = BaseErrorModel(
                        HttpStatusCode.Unauthorized.value,
                        ErrorType.DIALOG,
                        "Token is not valid or has expired"
                    )
                    call.respond(BaseModel<Nothing>(isSuccess = false, error = errorModel))
                }
            }
        }
    }
    post("/register") {
        try {
            var user = call.receive<User>()
            val collection = db().getCollection<User>()
            val searchedUser = collection.findOne(User::username eq user.username)

            if (searchedUser != null) {
                val errorMessage = JsonObject()
                errorMessage.addProperty("code", 409)
                errorMessage.addProperty("message", "User already exists")
                throw Exception(errorMessage.toString())
            }

            user = User(user.username, application.hashHMAC(user.password))
            collection.insertOne(user)

            val wrapper = BaseModel(isSuccess = true, data = user)
            call.respond(wrapper)
        } catch (e: Exception) {
            call.application.environment.log.error("exception handled here: ${e.message}")
            val error = e.catchMessage()
            var statusCode = HttpStatusCode.BadRequest.value
            var message = e.message
            if (error != null) {
                statusCode = error.code
                message = error.message
            }

            val wrapper = BaseModel<Any>(
                isSuccess = false, error = BaseErrorModel(
                    statusCode, ErrorType.TOAST, message = message!!
                )
            )

            call.respond(status = HttpStatusCode.fromValue(statusCode), wrapper)
        }
    }

    post("/login") {
        try {
            val user = call.receive<User>()
            val collection = db().getCollection<User>()
            val searchedUser = collection.findOne(User::username eq user.username)

            if (searchedUser == null) {
                val errorMessage = JsonObject()
                errorMessage.addProperty("code", 422)
                errorMessage.addProperty("message", "User not found")
                throw Exception(errorMessage.toString())
            }

            if (searchedUser.password != application.hashHMAC(user.password)) {
                val errorMessage = JsonObject()
                errorMessage.addProperty("code", 401)
                errorMessage.addProperty("message", "Incorrect password")
                throw Exception(errorMessage.toString())
            }

            val publicKey = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKey
            val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)
            val token = JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withClaim("username", user.username)
                .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))

            call.respond(BaseModel<Map<String, String>>(isSuccess = true, data = mapOf("token" to token)))
        } catch (e: Exception) {
            call.application.environment.log.error("exception handled here: ${e.message}")
            val error = e.catchMessage()
            var statusCode = HttpStatusCode.BadRequest.value
            var message = e.message
            if (error != null) {
                statusCode = error.code
                message = error.message
            }

            val wrapper = BaseModel<Any>(
                isSuccess = false, error = BaseErrorModel(
                    statusCode, ErrorType.TOAST, message = message!!
                )
            )

            call.respond(status = HttpStatusCode.fromValue(statusCode), wrapper)
        }
    }

    authenticate("auth-jwt") {
        get("/hello") {
            val principal = call.principal<JWTPrincipal>()
            val username = principal!!.payload.getClaim("username").asString()
            val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
            call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
        }
    }
    static(".well-known") {
        staticRootFolder = File("certs")
        file("jwks.json")
    }
}