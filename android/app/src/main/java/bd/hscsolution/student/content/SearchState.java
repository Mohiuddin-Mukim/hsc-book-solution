package bd.hscsolution.student.content;

public sealed interface SearchState permits SearchState.Idle, SearchState.Loading, SearchState.Success, SearchState.Error {
    record Idle() implements SearchState {}
    record Loading() implements SearchState {}
    record Success(CatalogModels.SearchPageResponse data) implements SearchState {}
    record Error(String message) implements SearchState {}
}
