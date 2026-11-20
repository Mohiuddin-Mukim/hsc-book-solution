package bd.hscsolution.student.auth;

public sealed interface AuthState permits AuthState.SignedOut, AuthState.Loading, AuthState.SignedIn,
        AuthState.Registered, AuthState.Error {
    record SignedOut() implements AuthState {}
    record Loading() implements AuthState {}
    record SignedIn() implements AuthState {}
    record Registered(String email) implements AuthState {}
    record Error(String message) implements AuthState {}
}
