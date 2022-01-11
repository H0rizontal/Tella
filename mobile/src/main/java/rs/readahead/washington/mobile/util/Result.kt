package rs.readahead.washington.mobile.util

sealed class Result<out T : Any?> {
    data class Success<out T : Any?>(val data: T) : Result<T>()
    data class Error(val exception: Exception?) : Result<Nothing>()
    data class Loading(val isLoading: Boolean) : Result<Nothing>()
}