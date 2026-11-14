package bd.hscsolution.media;

public final class MediaEnums {
    private MediaEnums() {}

    public enum LifecycleState {
        QUARANTINED, READY, REPLACED, ORPHANED, DELETED
    }

    public enum AttachmentRole {
        STATEMENT, ILLUSTRATION, GRAPH, DIAGRAM, OTHER
    }
}
