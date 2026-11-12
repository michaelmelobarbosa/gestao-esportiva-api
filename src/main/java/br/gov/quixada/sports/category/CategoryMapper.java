package br.gov.quixada.sports.category;

import br.gov.quixada.sports.category.dto.CategoryCreateRequest;
import br.gov.quixada.sports.category.dto.CategoryResponse;
import br.gov.quixada.sports.category.dto.CategorySummaryResponse;
import br.gov.quixada.sports.category.dto.CategoryUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "competition", ignore = true)
    @Mapping(target = "status", ignore = true)
    Category toEntity(@Valid CategoryCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "competition", ignore = true)
    @Mapping(target = "status", ignore = true)
    Category toEntity(@Valid CategoryUpdateRequest request);

    @Mapping(target = "competitionId", source = "competition.id")
    @Mapping(target = "competitionName", source = "competition.name")
    CategoryResponse toResponse(Category category);

    @Mapping(target = "competitionId", source = "competition.id")
    CategorySummaryResponse toSummary(Category category);

    List<CategorySummaryResponse> toSummaryList(List<Category> categories);
}
