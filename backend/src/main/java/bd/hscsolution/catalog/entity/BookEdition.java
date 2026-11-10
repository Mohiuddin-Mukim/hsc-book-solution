package bd.hscsolution.catalog.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="book_edition",uniqueConstraints=@UniqueConstraint(columnNames={"book_id","edition_label"})) public class BookEdition { @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="book_id") public Book book; @Column(name="edition_label",nullable=false,length=80) public String editionLabel; @Column(name="publication_year") public Short publicationYear; }
