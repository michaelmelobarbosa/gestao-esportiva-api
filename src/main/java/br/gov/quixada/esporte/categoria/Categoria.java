package br.gov.quixada.esporte.categoria;

import br.gov.quixada.esporte.categoria.exception.IdadeInvalidaException;
import br.gov.quixada.esporte.categoria.exception.CategoriaInativaException;
import br.gov.quixada.esporte.competicao.Competicao;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_categoria")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Integer idadeMinima;

    @Column(nullable = false)
    private Integer idadeMaxima;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_competicao", nullable = false)
    private Competicao competicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCategoria status;

    public void ativar() {
        if (this.status == StatusCategoria.ATIVO) return;
        this.status = StatusCategoria.ATIVO;
    }

    public void inativar() {
        if (this.status == StatusCategoria.INATIVO) return;
        this.status = StatusCategoria.INATIVO;
    }

    /** Atualiza dados mutáveis. Competição é imutável após a criação; bloqueia se INATIVO. */
    public void atualizarDados(String nome, Integer idadeMinima, Integer idadeMaxima) {
        if (this.status == StatusCategoria.INATIVO) {
            throw new CategoriaInativaException("Categoria está inativa, reative antes de editar");
        }
        validarIdades(idadeMinima, idadeMaxima);
        this.nome = nome;
        this.idadeMinima = idadeMinima;
        this.idadeMaxima = idadeMaxima;
    }

    public void validar() {
        validarIdades(this.idadeMinima, this.idadeMaxima);
    }

    void definirCompeticao(Competicao competicao) {
        this.competicao = competicao;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = StatusCategoria.ATIVO;
        }
    }

    private void validarIdades(Integer idadeMinima, Integer idadeMaxima) {
        if (idadeMinima != null && idadeMaxima != null && idadeMinima > idadeMaxima) {
            throw new IdadeInvalidaException("Idade mínima não pode ser maior que a idade máxima");
        }
    }
}
