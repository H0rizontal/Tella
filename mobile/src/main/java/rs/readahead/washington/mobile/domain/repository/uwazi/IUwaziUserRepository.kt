package rs.readahead.washington.mobile.domain.repository.uwazi

import io.reactivex.Single
import rs.readahead.washington.mobile.data.entity.uwazi.Language
import rs.readahead.washington.mobile.data.entity.uwazi.LanguageSettingsEntity
import rs.readahead.washington.mobile.data.entity.uwazi.SettingsResponse
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult

interface IUwaziUserRepository {
     fun login(server: UWaziUploadServer) : Single<LoginResponse>

     fun getTemplates(server: UWaziUploadServer) : Single<ListTemplateResult>

     fun getSettings(server: UWaziUploadServer) : Single<List<Language>>

     fun updateDefaultLanguage(languageSettingsEntity: LanguageSettingsEntity, server: UWaziUploadServer) : Single<SettingsResponse>

     fun submitEntity(uwaziEntityRow: UwaziEntityRow, server: UWaziUploadServer) : Single<UwaziEntityRow>
}