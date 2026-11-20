package bd.hscsolution.student.network;
public record ApiError(int httpCode, String code, String message) {}
