package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.equipe.Equipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_inscricao_equipe")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InscricaoEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_competicao", nullable = false)
    private Competicao competicao;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_equipe", nullable = false)
    private Equipe equipe;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataInscricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusInscricao status;

    /** Altera o status do ciclo de vida, idempotente quando igual. */
    public void alterarStatus(StatusInscricao novoStatus) {
        if (this.status == novoStatus) return;
        this.status = novoStatus;
    }

    void definirCompeticao(Competicao competicao) {
        this.competicao = competicao;
    }

    void definirCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    void definirEquipe(Equipe equipe) {
        this.equipe = equipe;
    }

    void definirStatus(StatusInscricao status) {
        this.status = status;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = StatusInscricao.PENDENTE;
        }
    }
}
