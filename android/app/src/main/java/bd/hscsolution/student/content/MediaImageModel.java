package bd.hscsolution.student.content;

public record MediaImageModel(
        String id, String role, int order, String caption, String altText,
        int width, int height, String mimeType, String checksumSha256, String deliveryUrl) {}
