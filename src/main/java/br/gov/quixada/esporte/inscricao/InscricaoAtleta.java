package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.atleta.Atleta;
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
@Table(name = "db_inscricao_atleta")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InscricaoAtleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_atleta", nullable = false)
    private Atleta atleta;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscricao_equipe", nullable = false)
    private InscricaoEquipe inscricaoEquipe;

    @Column(nullable = false, length = 3)
    private String numeroCamisa;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataInscricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusInscricao status;

    /** Atualiza o número da camisa. */
    public void atualizarDados(String numeroCamisa) {
        this.numeroCamisa = numeroCamisa;
    }

    /** Altera o status do ciclo de vida, idempotente quando igual. */
    public void alterarStatus(StatusInscricao novoStatus) {
        if (this.status == novoStatus) return;
        this.status = novoStatus;
    }

    void definirAtleta(Atleta atleta) {
        this.atleta = atleta;
    }

    void definirInscricaoEquipe(InscricaoEquipe inscricaoEquipe) {
        this.inscricaoEquipe = inscricaoEquipe;
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
