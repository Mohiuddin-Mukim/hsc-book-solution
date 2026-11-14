package bd.hscsolution.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

@Entity
@Getter
@Table(name = "solution")
public class Solution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", unique = true)
    public Question question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "current_version_id")
    public UUID currentVersionId;

    @Column(name = "published_version_id")
    public UUID publishedVersionId;

    @Column(name = "created_by")
    public UUID createdBy;
}
