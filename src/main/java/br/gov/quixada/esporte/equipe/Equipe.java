package br.gov.quixada.esporte.equipe;

import br.gov.quixada.esporte.clube.Clube;
import br.gov.quixada.esporte.equipe.exception.EquipeInativaException;
import br.gov.quixada.esporte.modalidade.Modalidade;
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
@Table(name = "db_equipe")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clube", nullable = false)
    private Clube clube;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modalidade", nullable = false)
    private Modalidade modalidade;

    @Column(nullable = false, length = 100)
    private String responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEquipe status;

    /** Atualiza dados mutáveis. Clube e modalidade são imutáveis após a criação; bloqueia se INATIVO. */
    public void atualizarDados(String nome, String responsavel) {
        if (this.status == StatusEquipe.INATIVO) {
            throw new EquipeInativaException("Equipe está inativa, reative antes de editar");
        }
        this.nome = nome;
        this.responsavel = responsavel;
    }

    public void ativar() {
        if (this.status == StatusEquipe.ATIVO) return;
        this.status = StatusEquipe.ATIVO;
    }

    public void inativar() {
        if (this.status == StatusEquipe.INATIVO) return;
        this.status = StatusEquipe.INATIVO;
    }

    void definirClube(Clube clube) {
        this.clube = clube;
    }

    void definirModalidade(Modalidade modalidade) {
        this.modalidade = modalidade;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = StatusEquipe.ATIVO;
        }
    }
}
