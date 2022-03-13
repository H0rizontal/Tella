package rs.readahead.washington.mobile.data.rest

import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import rs.readahead.washington.mobile.data.ParamsNetwork.COOKIE
import rs.readahead.washington.mobile.data.ParamsNetwork.X_REQUESTED_WITH
import rs.readahead.washington.mobile.data.entity.uwazi.*
import rs.readahead.washington.mobile.domain.entity.LoginResponse

interface IUwaziApi  {
     @GET
     fun getTemplates(
        @Url url : String,
        @Header(COOKIE) cookies : List<String>
    ): Single<TemplateResponse>

    @POST
    fun login(
        @Body loginEntity: LoginEntity,
        @Url url : String,
        @Header(X_REQUESTED_WITH) requested: String = "XMLHttpRequest"
    ) : Single<Response<LoginResponse>>


    @GET
    fun getSettings(
        @Url url : String,
        @Header(COOKIE) cookies : List<String>
    ) : Single<SettingsResponse>

    @GET
    fun getDictionary(
        @Url url : String,
        @Header(COOKIE) cookies : List<String>
    ) : Single<DictionaryResponse>


    @GET
    fun getTranslations(
        @Url url : String,
        @Header(COOKIE) cookies : List<String>
    ) : Single<TranslationResponse>


    @Multipart
    @POST
    fun submitEntity(
        @Part attachments : List<MultipartBody.Part?>,
        @Part("entity") entity: RequestBody,
        @Url url: String,
        @Header(COOKIE) cookies : List<String>,
        @Header(X_REQUESTED_WITH) requested: String = "XMLHttpRequest"
    ) : Single<UwaziEntityRow>
}

