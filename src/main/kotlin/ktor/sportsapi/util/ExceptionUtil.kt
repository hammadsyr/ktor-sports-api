package ktor.sportsapi.util

import io.ktor.client.plugins.*
import io.ktor.client.statement.*

//class MissingPageException(response: HttpResponse, cachedResponseText: String) :
//    ResponseException(response, cachedResponseText) {
//    override val message: String = "$cachedResponseTextMissing page: ${response.call.request.url}. " +
//            "Status: ${response.status}."
//}