package ktor.sportsapi.util

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import ktor.sportsapi.constant.AppConstant
import ktor.sportsapi.model.BaseErrorModel
import ktor.sportsapi.model.BaseModel
import ktor.sportsapi.model.ErrorType
import ktor.sportsapi.model.GeneralErrorModel
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun db(): CoroutineDatabase {
    val dbClient = KMongo.createClient().coroutine
    return dbClient.getDatabase(AppConstant.dbName)
}

val mainHttpClient = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 10000
    }
    expectSuccess = true
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.BODY
    }
}

suspend fun appErrorWrapper(call: ApplicationCall, status: HttpStatusCode) {
    val wrapper = BaseModel<Any>(
        isSuccess = false, error = BaseErrorModel(status.value, ErrorType.TOAST, message = status.description)
    )
    call.respond(status = status, wrapper)
}

suspend fun generalErrorWrapper(
    e: Exception,
    call: ApplicationCall,
    callback: ((e: ResponseException, message: String) -> Unit)? = null
) {

}

fun ResponseException.catchMessage(): String {
    if (message != null && message!!.isNotEmpty()) {
        var msg = message!!.split("Text: ")[1]
        msg = msg.replace("\"", "")
        return msg
    }

    return AppConstant.unknownError
}

fun Exception.catchMessage(): GeneralErrorModel? {
    if (message != null && message!!.isNotEmpty()) {
        return Gson().fromJson(message, GeneralErrorModel::class.java)
    }

    return null
}

fun Application.hashHMAC(stringToBeHashed: String): String {
    val key = environment.config.property("ktor.application.hashHmacKey").getString()
    val byteKey: ByteArray = key.toByteArray(StandardCharsets.UTF_8)
    val sha512Hmac = Mac.getInstance("HmacSHA512")
    val keySpec = SecretKeySpec(byteKey, "HmacSHA512")
    sha512Hmac.init(keySpec)
    val macData = sha512Hmac.doFinal(stringToBeHashed.toByteArray(StandardCharsets.UTF_8))
    return Base64.getEncoder().encodeToString(macData)
}



