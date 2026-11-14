package bd.hscsolution.media;

import bd.hscsolution.catalog.entity.QuestionVersion;
import bd.hscsolution.catalog.entity.SolutionStep;
import bd.hscsolution.catalog.repository.QuestionVersionRepository;
import bd.hscsolution.catalog.repository.SolutionStepRepository;
import bd.hscsolution.security.AuditService;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaService {
    private final MediaAssetRepository assets;
    private final QuestionMediaAttachmentRepository qAttachments;
    private final SolutionStepMediaAttachmentRepository sAttachments;
    private final QuestionVersionRepository qVersions;
    private final SolutionStepRepository steps;
    private final MediaValidationService validator;
    private final MediaStorageRouter storage;
    private final MediaProperties props;
    private final AuditService audit;

    public MediaService(MediaAssetRepository assets, QuestionMediaAttachmentRepository qAttachments,
                        SolutionStepMediaAttachmentRepository sAttachments, QuestionVersionRepository qVersions,
                        SolutionStepRepository steps, MediaValidationService validator, MediaStorageRouter storage,
                        MediaProperties props, AuditService audit) {
        this.assets=assets; this.qAttachments=qAttachments; this.sAttachments=sAttachments;
        this.qVersions=qVersions; this.steps=steps; this.validator=validator; this.storage=storage;
        this.props=props; this.audit=audit;
    }

    @Transactional
    public MediaDtos.MediaAssetResponse upload(MultipartFile file, String caption, String altText, UUID actor) {
        MediaValidationService.ValidatedMedia v;
        try { v=validator.quarantine(file); } catch(IOException e){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Media upload failed",e); }
        String key="images/"+java.time.LocalDate.now()+"/"+UUID.randomUUID()+"."+v.extension();
        MediaAsset a=new MediaAsset();
        a.storageKey=key; a.mimeType=v.mimeType(); a.fileName=v.originalName(); a.byteSize=v.byteSize();
        a.checksumSha256=v.checksumSha256(); a.width=v.width(); a.height=v.height();
        a.caption=trim(caption); a.altText=trim(altText); a.createdBy=actor;
        a.lifecycleState=MediaEnums.LifecycleState.QUARANTINED; a.storageBackend=storage.backend(); a.originalExtension=v.extension();
        assets.saveAndFlush(a);
        try {
            storage.put(key,v.quarantine(),v.mimeType());
            a.lifecycleState=MediaEnums.LifecycleState.READY;
            assets.save(a);
            audit.record(actor,"MEDIA_UPLOAD","MEDIA_ASSET",a.id,null,Map.of("checksum",a.checksumSha256,"bytes",a.byteSize));
            return dto(a,"",0);
        } catch(Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Media object storage failed; asset remains quarantined",e);
        } finally {
            try { Files.deleteIfExists(v.quarantine()); } catch(IOException ignored){}
        }
    }

    @Transactional
    public MediaDtos.MediaAssetResponse attachQuestion(UUID versionId, MediaDtos.AttachRequest r, UUID actor) {
        QuestionVersion v=or404(qVersions.findById(versionId),"Question version");
        assertMutableQuestion(v);
        MediaAsset a=ready(r.assetId());
        if(qAttachments.findByQuestionVersionIdAndMediaAssetId(versionId,a.id).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Media is already attached");
        int order=r.displayOrder()==null?qAttachments.findByQuestionVersionIdOrderByDisplayOrderAsc(versionId).size():r.displayOrder();
        normalizeQuestionOrders(versionId);
        qAttachments.save(new QuestionMediaAttachment(v,a,order,role(r.role())));
        applyMeta(a,r);
        audit.record(actor,"MEDIA_ATTACH","QUESTION_VERSION",versionId,null,Map.of("mediaAssetId",a.id,"order",order));
        return dto(a,role(r.role()),order,qAttachments.findByQuestionVersionIdOrderByDisplayOrderAsc(versionId).size());
    }

    @Transactional
    public MediaDtos.MediaAssetResponse attachStep(UUID stepId, MediaDtos.AttachRequest r, UUID actor) {
        SolutionStep step=or404(steps.findById(stepId),"Solution step");
        assertMutableStep(step);
        MediaAsset a=ready(r.assetId());
        if(sAttachments.findBySolutionStepIdAndMediaAssetId(stepId,a.id).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Media is already attached");
        int order=r.displayOrder()==null?sAttachments.findBySolutionStepIdOrderByDisplayOrderAsc(stepId).size():r.displayOrder();
        sAttachments.save(new SolutionStepMediaAttachment(step,a,order,role(r.role())));
        applyMeta(a,r);
        audit.record(actor,"MEDIA_ATTACH","SOLUTION_STEP",stepId,null,Map.of("mediaAssetId",a.id,"order",order));
        return dto(a,role(r.role()),order,sAttachments.findBySolutionStepIdOrderByDisplayOrderAsc(stepId).size());
    }

    @Transactional
    public void detachQuestion(UUID versionId, UUID assetId, UUID actor) {
        QuestionVersion v=or404(qVersions.findById(versionId),"Question version"); assertMutableQuestion(v);
        qAttachments.deleteByQuestionVersionIdAndMediaAssetId(versionId,assetId);
        normalizeQuestionOrders(versionId);
        markOrphan(assetId);
        audit.record(actor,"MEDIA_DETACH","QUESTION_VERSION",versionId,null,Map.of("mediaAssetId",assetId));
    }

    @Transactional
    public void detachStep(UUID stepId, UUID assetId, UUID actor) {
        SolutionStep s=or404(steps.findById(stepId),"Solution step"); assertMutableStep(s);
        sAttachments.deleteBySolutionStepIdAndMediaAssetId(stepId,assetId);
        normalizeStepOrders(stepId);
        markOrphan(assetId);
        audit.record(actor,"MEDIA_DETACH","SOLUTION_STEP",stepId,null,Map.of("mediaAssetId",assetId));
    }

    @Transactional
    public void reorderQuestion(UUID versionId, List<MediaDtos.ReorderItem> items, UUID actor) {
        QuestionVersion v=or404(qVersions.findById(versionId),"Question version"); assertMutableQuestion(v);
        List<QuestionMediaAttachment> current=qAttachments.findByQuestionVersionIdOrderByDisplayOrderAsc(versionId);
        if(current.size()!=items.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reorder must include every attached asset");
        Map<UUID,Integer> wanted=new HashMap<>();
        for(var i:items) wanted.put(i.assetId(),i.displayOrder());
        if(wanted.size()!=items.size() || current.stream().anyMatch(a->!wanted.containsKey(a.mediaAsset.id)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reorder asset set does not match current attachments");
        for(var a:current) a.displayOrder=wanted.get(a.mediaAsset.id);
        qAttachments.saveAll(current);
        audit.record(actor,"MEDIA_REORDER","QUESTION_VERSION",versionId,null,Map.of("count",items.size()));
    }

    @Transactional
    public void reorderStep(UUID stepId, List<MediaDtos.ReorderItem> items, UUID actor) {
        SolutionStep s=or404(steps.findById(stepId),"Solution step"); assertMutableStep(s);
        List<SolutionStepMediaAttachment> current=sAttachments.findBySolutionStepIdOrderByDisplayOrderAsc(stepId);
        if(current.size()!=items.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reorder must include every attached asset");
        Map<UUID,Integer> wanted=new HashMap<>(); for(var i:items) wanted.put(i.assetId(),i.displayOrder());
        if(wanted.size()!=items.size() || current.stream().anyMatch(a->!wanted.containsKey(a.mediaAsset.id)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reorder asset set does not match current attachments");
        for(var a:current) a.displayOrder=wanted.get(a.mediaAsset.id);
        sAttachments.saveAll(current);
        audit.record(actor,"MEDIA_REORDER","SOLUTION_STEP",stepId,null,Map.of("count",items.size()));
    }

    @Transactional
    public MediaDtos.MediaAssetResponse updateMetadata(UUID assetId,String caption,String altText,UUID actor) {
        MediaAsset a=ready(assetId);
        assertAssetMutable(a);
        a.caption=trim(caption); a.altText=trim(altText); assets.save(a);
        audit.record(actor,"MEDIA_METADATA_EDIT","MEDIA_ASSET",assetId,null,Map.of("caption",a.caption==null?"":a.caption,"altText",a.altText==null?"":a.altText));
        return dto(a,null,0);
    }

    @Transactional
    public MediaDtos.ReplacementResponse replaceQuestion(UUID versionId, UUID oldAssetId, MultipartFile file,
                                                         String caption,String altText, String role, UUID actor) {
        QuestionVersion v=or404(qVersions.findById(versionId),"Question version"); assertMutableQuestion(v);
        MediaDtos.MediaAssetResponse old=attachmentQuestion(versionId,oldAssetId);
        MediaDtos.MediaAssetResponse fresh=upload(file,caption,altText,actor);
        detachQuestion(versionId,oldAssetId,actor);
        attachQuestion(versionId,new MediaDtos.AttachRequest(fresh.id(),role,old.order(),caption,altText),actor);
        MediaDtos.MediaAssetResponse now=asset(fresh.id(),role,old.order());
        assets.findById(oldAssetId).ifPresent(x->{x.lifecycleState=MediaEnums.LifecycleState.REPLACED;assets.save(x);});
        return new MediaDtos.ReplacementResponse(old,now);
    }

    @Transactional
    public MediaDtos.MediaAssetResponse asset(UUID id,String role,Integer order) {
        MediaAsset a=ready(id); long usage=assets.questionUsage(id)+assets.stepUsage(id);
        return dto(a,role,order,usage);
    }

    public InputStream openAuthorized(UUID assetId, UUID actor, boolean admin) {
        MediaAsset a=ready(assetId);
        if(!admin && !isPublishedReference(assetId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Media not published");
        try { return storage.open(a.storageKey); }
        catch(IOException e){ throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Media object not found"); }
    }

    private boolean isPublishedReference(UUID assetId) {
        return qAttachments.findAll().stream().anyMatch(a ->
                Objects.equals(a.getMediaAsset().getId(), assetId)
                        && a.getQuestionVersion().getQuestion().getPublishedVersionId() != null
                        && Objects.equals(a.getQuestionVersion().getQuestion().getPublishedVersionId(), a.getQuestionVersion().getId()))
                || sAttachments.findAll().stream().anyMatch(a ->
                Objects.equals(a.getMediaAsset().getId(), assetId)
                        && a.getSolutionStep().getSolutionVersion().getSolution().getPublishedVersionId() != null
                        && Objects.equals(a.getSolutionStep().getSolutionVersion().getSolution().getPublishedVersionId(), a.getSolutionStep().getSolutionVersion().getId()));
    }

    @Transactional
    public int cleanupOrphans(int limit, UUID actor) {
        int n=0;
        for(MediaAsset a:assets.findAll()) {
            if(n>=Math.max(1,Math.min(limit,500))) break;
            long usage=assets.questionUsage(a.id)+assets.stepUsage(a.id);
            if(usage==0 && (a.lifecycleState==MediaEnums.LifecycleState.READY || a.lifecycleState==MediaEnums.LifecycleState.ORPHANED)) {
                try { storage.delete(a.storageKey); } catch(IOException ignored) { continue; }
                a.lifecycleState=MediaEnums.LifecycleState.DELETED; assets.save(a); n++;
            }
        }
        if(n>0) audit.record(actor,"MEDIA_ORPHAN_CLEANUP","MEDIA_ASSET",null,null,Map.of("deletedCount",n));
        return n;
    }

    private MediaDtos.MediaAssetResponse attachmentQuestion(UUID v,UUID asset){
        var a=qAttachments.findByQuestionVersionIdAndMediaAssetId(v,asset).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Attachment not found"));
        return dto(a.mediaAsset,a.attachmentRole,a.displayOrder,assets.questionUsage(asset)+assets.stepUsage(asset));
    }
    private void applyMeta(MediaAsset a,MediaDtos.AttachRequest r){if(r.caption()!=null)a.caption=trim(r.caption());if(r.altText()!=null)a.altText=trim(r.altText());assets.save(a);}
    private void markOrphan(UUID id){assets.findById(id).ifPresent(a->{if(assets.questionUsage(id)+assets.stepUsage(id)==0&&a.lifecycleState==MediaEnums.LifecycleState.READY)a.lifecycleState=MediaEnums.LifecycleState.ORPHANED;assets.save(a);});}
    private void normalizeQuestionOrders(UUID v){var x=qAttachments.findByQuestionVersionIdOrderByDisplayOrderAsc(v);for(int i=0;i<x.size();i++)x.get(i).displayOrder=i;qAttachments.saveAll(x);}
    private void normalizeStepOrders(UUID v){var x=sAttachments.findBySolutionStepIdOrderByDisplayOrderAsc(v);for(int i=0;i<x.size();i++)x.get(i).displayOrder=i;sAttachments.saveAll(x);}
    private void assertMutableQuestion(QuestionVersion v){if(v.question.publishedVersionId!=null&&v.question.publishedVersionId.equals(v.id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Published question version is immutable");}
    private void assertMutableStep(SolutionStep s) {
        UUID publishedId = s.getSolutionVersion().getSolution().getPublishedVersionId();
        if (publishedId != null && Objects.equals(publishedId, s.getSolutionVersion().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Published solution version is immutable");
        }
    }
    private void assertAssetMutable(MediaAsset a){if(assets.questionUsage(a.id)+assets.stepUsage(a.id)>0){throw new ResponseStatusException(HttpStatus.CONFLICT,"Attached media metadata must be edited through its attachment version");}}
    private MediaAsset ready(UUID id){MediaAsset a=or404(assets.findById(id),"Media asset");if(a.lifecycleState!=MediaEnums.LifecycleState.READY)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Media asset is not ready");return a;}
    private String role(String r){return r==null||r.isBlank()?MediaEnums.AttachmentRole.ILLUSTRATION.name():r.trim().toUpperCase(Locale.ROOT);}
    private String trim(String x){return x==null||x.isBlank()?null:x.trim();}
    private <T>T or404(Optional<T> x,String w){return x.orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,w+" not found"));}
    private MediaDtos.MediaAssetResponse dto(MediaAsset a,String role,int order){return dto(a,role,order,assets.questionUsage(a.id)+assets.stepUsage(a.id));}
    private MediaDtos.MediaAssetResponse dto(MediaAsset a,String role,int order,long usage){
        return new MediaDtos.MediaAssetResponse(a.id,role==null?"":role,order,a.caption,a.altText,a.width==null?0:a.width,a.height==null?0:a.height,a.byteSize,a.mimeType,a.checksumSha256,a.lifecycleState.name(),"/api/v1/media/"+a.id,usage);
    }
}
