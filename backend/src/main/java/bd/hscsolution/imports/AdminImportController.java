
package bd.hscsolution.imports;

import bd.hscsolution.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/imports")
public class AdminImportController {
    private final BulkImportService service;
    public AdminImportController(BulkImportService service){this.service=service;}

    private UUID actor(AuthPrincipal p){return UUID.fromString(p.userId());}

    @PostMapping("/json")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public ImportDtos.ImportJobResponse paste(@Valid @RequestBody ImportDtos.JsonPasteRequest r,
                                              @AuthenticationPrincipal AuthPrincipal p){
        return service.stageJsonPaste(r,actor(p));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public ImportDtos.ImportJobResponse upload(@RequestParam MultipartFile file,
                                               @RequestParam(defaultValue="CREATE_ONLY") String mode,
                                               @RequestParam(defaultValue="false") boolean dryRun,
                                               @RequestParam(required=false) String idempotencyKey,
                                               @AuthenticationPrincipal AuthPrincipal p){
        if(file==null||file.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Import file is empty");
        String name=file.getOriginalFilename()==null?"import":file.getOriginalFilename().toLowerCase();
        String type=name.endsWith(".zip")?"ZIP":name.endsWith(".xlsx")?"XLSX":name.endsWith(".csv")?"CSV":name.endsWith(".json")?"JSON":null;
        if(type==null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Supported formats: JSON, CSV, XLSX, ZIP");
        try{return service.stageBytes(file.getBytes(),type,file.getOriginalFilename(),ImportDtos.ImportMode.valueOf(mode.toUpperCase()),dryRun,idempotencyKey,actor(p));}
        catch(IllegalArgumentException e){throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid import mode",e);}
        catch(java.io.IOException e){throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Unable to read import file",e);}
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public ImportDtos.ImportPreviewResponse preview(@PathVariable UUID jobId){return service.preview(jobId);}

    @PostMapping("/{jobId}/commit")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public ImportDtos.CommitResponse commit(@PathVariable UUID jobId,
                                            @RequestParam(defaultValue="false") boolean submitForReview,
                                            @AuthenticationPrincipal AuthPrincipal p){
        return service.commit(jobId,actor(p),submitForReview);
    }

    @PostMapping("/{jobId}/retry")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public ImportDtos.CommitResponse retry(@PathVariable UUID jobId,
                                           @RequestParam(defaultValue="false") boolean submitForReview,
                                           @AuthenticationPrincipal AuthPrincipal p){
        return service.retry(jobId,actor(p),submitForReview);
    }

    @GetMapping(value="/{jobId}/errors.csv",produces="text/csv")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public ResponseEntity<byte[]> errors(@PathVariable UUID jobId){
        StringBuilder csv=new StringBuilder("rowNo,externalId,fieldName,errorCode,severity,message\n");
        for(var r:service.errors(jobId)){
            csv.append(r.rowNo()).append(',').append(q(r.externalId())).append(',').append(q(r.fieldName())).append(',')
                    .append(q(r.errorCode())).append(',').append(q(r.severity())).append(',').append(q(r.message())).append('\n');
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=import-errors-"+jobId+".csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static String q(String x){if(x==null)return "";return "\""+x.replace("\"","\"\"")+"\"";}
}
