package bd.hscsolution.catalog.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="subject") public class Subject { @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @Column(nullable=false,unique=true,length=50) public String code; @Column(nullable=false,length=160) public String name; @Column(name="display_order",nullable=false) public int displayOrder; @Column(name="is_active",nullable=false) public boolean active=true; }
