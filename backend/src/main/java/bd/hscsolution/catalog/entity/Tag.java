package bd.hscsolution.catalog.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="tag") public class Tag { @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @Column(nullable=false,unique=true,length=80) public String code; @Column(nullable=false,length=120) public String label; }
