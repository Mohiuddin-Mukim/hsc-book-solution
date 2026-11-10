package bd.hscsolution.catalog.dto;
import java.util.*;
public record ContentPageResponse<T>(List<T> items,int page,int size,long totalItems,int totalPages){ public static <T> ContentPageResponse<T> of(org.springframework.data.domain.Page<T> p){return new ContentPageResponse<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());} }
