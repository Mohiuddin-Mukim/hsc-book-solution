package bd.hscsolution.media;

import bd.hscsolution.security.AuthPrincipal;
import jakarta.validation.Valid;
import java.io.*;
import java.util.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MediaController {
    private final MediaService service;
    public MediaController(MediaService service){this.service=service;}

    private UUID actor(AuthPrincipal p){return UUID.fromString(p.userId());}

    @PostMapping(value="/api/v1/admin/media/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public MediaDtos.MediaAssetResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required=false) String caption,
            @RequestParam(required=false) String altText,
            @AuthenticationPrincipal AuthPrincipal p){return service.upload(file,caption,altText,actor(p));}

    @PostMapping("/api/v1/admin/question-versions/{versionId}/media")
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public MediaDtos.MediaAssetResponse attachQuestion(@PathVariable UUID versionId,@Valid @RequestBody MediaDtos.AttachRequest r,@AuthenticationPrincipal AuthPrincipal p){
        return service.attachQuestion(versionId,r,actor(p));
    }

    @DeleteMapping("/api/v1/admin/question-versions/{versionId}/media/{assetId}")
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public ResponseEntity<Void> detachQuestion(@PathVariable UUID versionId,@PathVariable UUID assetId,@AuthenticationPrincipal AuthPrincipal p){
        service.detachQuestion(versionId,assetId,actor(p)); return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/admin/question-versions/{versionId}/media/reorder")
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public ResponseEntity<Void> reorderQuestion(@PathVariable UUID versionId,@RequestBody List<MediaDtos.ReorderItem> items,@AuthenticationPrincipal AuthPrincipal p){
        service.reorderQuestion(versionId,items,actor(p)); return ResponseEntity.noContent().build();
    }

    @PostMapping(value="/api/v1/admin/question-versions/{versionId}/media/{assetId}/replace",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public MediaDtos.ReplacementResponse replaceQuestion(@PathVariable UUID versionId,@PathVariable UUID assetId,
            @RequestPart("file") MultipartFile file,@RequestParam(required=false) String caption,@RequestParam(required=false) String altText,
            @RequestParam(required=false) String role,@AuthenticationPrincipal AuthPrincipal p){
        return service.replaceQuestion(versionId,assetId,file,caption,altText,role,actor(p));
    }

    @PostMapping("/api/v1/admin/solution-steps/{stepId}/media")
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public MediaDtos.MediaAssetResponse attachStep(@PathVariable UUID stepId,@RequestBody MediaDtos.AttachRequest r,@AuthenticationPrincipal AuthPrincipal p){
        return service.attachStep(stepId,r,actor(p));
    }

    @DeleteMapping("/api/v1/admin/solution-steps/{stepId}/media/{assetId}")
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public ResponseEntity<Void> detachStep(@PathVariable UUID stepId,@PathVariable UUID assetId,@AuthenticationPrincipal AuthPrincipal p){
        service.detachStep(stepId,assetId,actor(p)); return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/admin/solution-steps/{stepId}/media/reorder")
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public ResponseEntity<Void> reorderStep(@PathVariable UUID stepId,@RequestBody List<MediaDtos.ReorderItem> items,@AuthenticationPrincipal AuthPrincipal p){
        service.reorderStep(stepId,items,actor(p)); return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/media/{assetId}")
    @PreAuthorize("hasAuthority('PERM_MEDIA_MANAGE')")
    public MediaDtos.MediaAssetResponse adminAsset(@PathVariable UUID assetId){return service.asset(assetId,null,0);}

    @GetMapping("/api/v1/media/{assetId}")
    public ResponseEntity<InputStreamResource> delivery(@PathVariable UUID assetId,@AuthenticationPrincipal AuthPrincipal p) {
        boolean admin=p!=null;
        InputStream in=service.openAuthorized(assetId,p==null?null:actor(p),admin);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1,java.util.concurrent.TimeUnit.HOURS).cachePrivate())
                .contentType(MediaType.parseMediaType(service.asset(assetId,null,0).mimeType()))
                .body(new InputStreamResource(in));
    }
}
