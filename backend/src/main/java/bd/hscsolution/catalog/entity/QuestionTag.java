package bd.hscsolution.catalog.entity;
import jakarta.persistence.*; import java.io.Serializable; import java.util.*;
@Entity @Table(name="question_tag") @IdClass(QuestionTag.Key.class)
public class QuestionTag { @Id @Column(name="question_version_id") public UUID questionVersionId; @Id @Column(name="tag_id") public UUID tagId; public QuestionTag(){} public QuestionTag(Key k){this.questionVersionId=k.questionVersionId;this.tagId=k.tagId;} public static class Key implements Serializable { public UUID questionVersionId; public UUID tagId; public Key(){} public Key(UUID q,UUID t){questionVersionId=q;tagId=t;} public boolean equals(Object o){if(this==o)return true;if(!(o instanceof Key k))return false;return Objects.equals(questionVersionId,k.questionVersionId)&&Objects.equals(tagId,k.tagId);} public int hashCode(){return Objects.hash(questionVersionId,tagId);} }
}
