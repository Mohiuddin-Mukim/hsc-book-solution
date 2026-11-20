package bd.hscsolution.student.network;
public final class ApiException extends Exception {
    private final ApiError error;
    public ApiException(ApiError error) { super(error.message()); this.error = error; }
    public ApiError error() { return error; }
}
