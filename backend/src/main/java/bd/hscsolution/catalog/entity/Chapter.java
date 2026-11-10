package bd.hscsolution.catalog.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="chapter",uniqueConstraints=@UniqueConstraint(columnNames={"paper_id","code"})) public class Chapter { @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="paper_id") public Paper paper; @Column(nullable=false,length=40) public String code; @Column(nullable=false,length=200) public String title; @Column(name="display_order",nullable=false) public int displayOrder; }
