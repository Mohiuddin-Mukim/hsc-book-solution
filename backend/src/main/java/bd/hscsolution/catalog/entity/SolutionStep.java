package bd.hscsolution.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Table(name = "solution_step", uniqueConstraints = @UniqueConstraint(columnNames = {"solution_version_id", "step_no"}))
public class SolutionStep {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solution_version_id")
    public SolutionVersion solutionVersion;

    @Column(name = "step_no", nullable = false)
    public int stepNo;

    @Column(length = 200)
    public String title;

    @Column(nullable = false, columnDefinition = "text")
    public String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    public String metadata = "{}";
}
