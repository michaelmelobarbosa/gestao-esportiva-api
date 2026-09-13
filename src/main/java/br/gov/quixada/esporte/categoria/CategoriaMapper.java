package br.gov.quixada.esporte.categoria;

import br.gov.quixada.esporte.categoria.dto.CategoriaCreateRequest;
import br.gov.quixada.esporte.categoria.dto.CategoriaResponse;
import br.gov.quixada.esporte.categoria.dto.CategoriaResumoResponse;
import br.gov.quixada.esporte.categoria.dto.CategoriaUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "competicao", ignore = true)
    Categoria toEntity(@Valid CategoriaCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "competicao", ignore = true)
    Categoria toEntity(@Valid CategoriaUpdateRequest request);

    @Mapping(target = "competicaoId", source = "competicao.id")
    @Mapping(target = "competicaoNome", source = "competicao.nome")
    CategoriaResponse toResponse(Categoria categoria);

    @Mapping(target = "competicaoId", source = "competicao.id")
    CategoriaResumoResponse toResumo(Categoria categoria);

    List<CategoriaResumoResponse> toResumoList(List<Categoria> categorias);
}
