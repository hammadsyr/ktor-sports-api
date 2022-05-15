package ktor.sportsapi.model


data class BaseModel<T>(
    val isSuccess: Boolean,
    val data: T? = null,
    val error: BaseErrorModel? = null
)

data class BaseErrorModel(
    val errorCode: Int,
    val errorType: ErrorType,
    var title: String = "",
    var message: String = ""
)

data class GeneralErrorModel(
    val code: Int,
    var message: String = ""
)

enum class ErrorType {
    DIALOG,
    TOAST
}