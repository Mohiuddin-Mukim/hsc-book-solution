package bd.hscsolution.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

@Entity
@Getter
@Table(name = "solution_version", uniqueConstraints = @UniqueConstraint(columnNames = {"solution_id", "version_no", "language_code"}))
public class SolutionVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solution_id")
    public Solution solution;

    @Column(name = "version_no", nullable = false)
    public int versionNo;

    @Column(name = "language_code", nullable = false, length = 10)
    public String languageCode;

    @Column(columnDefinition = "text")
    public String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    public String metadata = "{}";

    @Column(name = "created_by")
    public UUID createdBy;

    @OneToMany(mappedBy = "solutionVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNo ASC")
    public List<SolutionStep> steps = new ArrayList<>();
}
