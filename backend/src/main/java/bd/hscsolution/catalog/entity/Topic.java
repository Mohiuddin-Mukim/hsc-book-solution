package bd.hscsolution.catalog.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="topic",uniqueConstraints=@UniqueConstraint(columnNames={"chapter_id","code"})) public class Topic { @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="chapter_id") public Chapter chapter; @Column(nullable=false,length=40) public String code; @Column(nullable=false,length=200) public String title; @Column(name="display_order",nullable=false) public int displayOrder; }
