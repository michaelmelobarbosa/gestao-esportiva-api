package br.gov.quixada.esporte.partida;

import br.gov.quixada.esporte.inscricao.InscricaoEquipe;
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

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_partida")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false)
    private Integer rodada;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 150)
    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPartida status;

    private Integer golsMandante;

    private Integer golsVisitante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscricao_mandante", nullable = false)
    private InscricaoEquipe inscricaoMandante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscricao_visitante", nullable = false)
    private InscricaoEquipe inscricaoVisitante;

    public void definirInscricoes(InscricaoEquipe mandante, InscricaoEquipe visitante) {
        this.inscricaoMandante = mandante;
        this.inscricaoVisitante = visitante;
    }

    public void atualizarDados(Integer rodada, LocalDateTime dataHora, String local,
                               InscricaoEquipe mandante, InscricaoEquipe visitante) {
        this.rodada = rodada;
        this.dataHora = dataHora;
        this.local = local;
        this.inscricaoMandante = mandante;
        this.inscricaoVisitante = visitante;
    }

    public void alterarStatus(StatusPartida novoStatus) {
        if (this.status == novoStatus) {
            return;
        }
        this.status = novoStatus;
    }

    public void registrarResultado(Integer golsMandante, Integer golsVisitante) {
        this.golsMandante = golsMandante;
        this.golsVisitante = golsVisitante;
        this.status = StatusPartida.FINALIZADA;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = StatusPartida.AGENDADA;
        }
    }
}
