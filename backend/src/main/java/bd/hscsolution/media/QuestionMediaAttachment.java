package bd.hscsolution.media;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
@Table(name="question_media_attachment")
public class QuestionMediaAttachment {
    @EmbeddedId public Key id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @MapsId("questionVersionId")
    @JoinColumn(name="question_version_id") public bd.hscsolution.catalog.entity.QuestionVersion questionVersion;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @MapsId("mediaAssetId")
    @JoinColumn(name="media_asset_id") public MediaAsset mediaAsset;
    @Column(name="display_order",nullable=false) public int displayOrder;
    @Column(name="attachment_role",nullable=false,length=30) public String attachmentRole;

    public QuestionMediaAttachment() {}
    public QuestionMediaAttachment(bd.hscsolution.catalog.entity.QuestionVersion qv, MediaAsset asset, int order, String role) {
        this.questionVersion=qv; this.mediaAsset=asset; this.displayOrder=order; this.attachmentRole=role;
        this.id=new Key(qv.id,asset.id);
    }
    @Embeddable public record Key(UUID questionVersionId, UUID mediaAssetId) {}
}
