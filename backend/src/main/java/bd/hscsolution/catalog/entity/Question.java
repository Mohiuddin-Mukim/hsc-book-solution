package bd.hscsolution.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

@Entity
@Getter
@Table(name = "question")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id")
    public Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    public QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    public Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "current_version_id")
    public UUID currentVersionId;

    @Column(name = "published_version_id")
    public UUID publishedVersionId;

    @Column(name = "created_by")
    public UUID createdBy;

    // Question.java ক্লাসে
    public UUID getId() {
        return this.id;
    }
}
