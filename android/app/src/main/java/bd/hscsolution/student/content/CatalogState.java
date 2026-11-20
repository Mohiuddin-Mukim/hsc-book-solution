package bd.hscsolution.student.content;

public sealed interface CatalogState permits CatalogState.Loading, CatalogState.Success, CatalogState.Error {
    record Loading() implements CatalogState {}
    record Success(Object data) implements CatalogState {}
    record Error(String message) implements CatalogState {}
}
