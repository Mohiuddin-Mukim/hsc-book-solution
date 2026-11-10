package bd.hscsolution.catalog.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="book") public class Book { @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="subject_id") public Subject subject; @Column(nullable=false,length=240) public String title; @Column(length=160) public String publisher; @Column(length=30) public String isbn; @Column(nullable=false,columnDefinition="jsonb") public String metadata="{}"; }
