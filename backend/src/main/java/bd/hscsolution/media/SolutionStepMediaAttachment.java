package bd.hscsolution.media;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
@Table(name="solution_step_media_attachment")
public class SolutionStepMediaAttachment {
    @EmbeddedId public Key id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @MapsId("solutionStepId")
    @JoinColumn(name="solution_step_id") public bd.hscsolution.catalog.entity.SolutionStep solutionStep;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @MapsId("mediaAssetId")
    @JoinColumn(name="media_asset_id") public MediaAsset mediaAsset;
    @Column(name="display_order",nullable=false) public int displayOrder;
    @Column(name="attachment_role",nullable=false,length=30) public String attachmentRole;

    public SolutionStepMediaAttachment() {}
    public SolutionStepMediaAttachment(bd.hscsolution.catalog.entity.SolutionStep step, MediaAsset asset, int order, String role) {
        this.solutionStep=step; this.mediaAsset=asset; this.displayOrder=order; this.attachmentRole=role;
        this.id=new Key(step.id,asset.id);
    }
    @Embeddable public record Key(UUID solutionStepId, UUID mediaAssetId) {}
}
