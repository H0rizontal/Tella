package rs.readahead.washington.mobile.domain.repository.uwazi;

import org.javarosa.core.model.FormDef;

import java.util.List;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate;
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult;

public interface ICollectUwaziTemplatesRepository {
     Single<List<CollectTemplate>> listBlankTemplates();
     Single<List<CollectTemplate>> listFavoriteTemplates();
     Single<ListTemplateResult> updateBlankTemplates(ListTemplateResult listTemplateResult);
     Single<ListTemplateResult> updateBlankTemplatesIfNeeded(ListTemplateResult listTemplateResult);
     Single<CollectTemplate> updateBlankTemplate(CollectTemplate template);
}
