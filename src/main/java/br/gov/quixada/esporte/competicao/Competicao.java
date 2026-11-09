package br.gov.quixada.esporte.competicao;

import br.gov.quixada.esporte.competicao.exception.CompeticaoNaoEditavelException;
import br.gov.quixada.esporte.competicao.exception.PeriodoInvalidoException;
import br.gov.quixada.esporte.modalidade.Modalidade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_competicao")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Competicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Integer ano;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modalidade", nullable = false)
    private Modalidade modalidade;

    @Column(nullable = false)
    private LocalDate dataInicioInscricao;

    @Column(nullable = false)
    private LocalDate dataFimInscricao;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCompeticao status;

    /** Cancelamento manual (terminal). */
    public void cancelar() {
        this.status = StatusCompeticao.CANCELADA;
    }

    /** Recalcula o status com base nas datas; CANCELADA é terminal. */
    public void sincronizarStatus(LocalDate hoje) {
        if (this.status == StatusCompeticao.CANCELADA) {
            return;
        }
        this.status = calcularStatus(hoje);
    }

    public StatusCompeticao calcularStatus(LocalDate hoje) {
        if (this.status == StatusCompeticao.CANCELADA) {
            return StatusCompeticao.CANCELADA;
        }
        if (dataInicioInscricao == null || dataFimInscricao == null || dataInicio == null || dataFim == null) {
            return this.status != null ? this.status : StatusCompeticao.PLANEJAMENTO;
        }
        if (hoje.isAfter(this.dataFim)) {
            return StatusCompeticao.FINALIZADA;
        }
        if (!hoje.isBefore(this.dataInicio)) {
            return StatusCompeticao.EM_ANDAMENTO;
        }
        if (hoje.isAfter(this.dataFimInscricao)) {
            return StatusCompeticao.INSCRICOES_ENCERRADAS;
        }
        if (!hoje.isBefore(this.dataInicioInscricao)) {
            return StatusCompeticao.INSCRICOES_ABERTAS;
        }
        return StatusCompeticao.PLANEJAMENTO;
    }

    /**
     * Atualiza dados mutáveis. Modalidade imutável após a criação;
     * bloqueia edição se FINALIZADA ou CANCELADA.
     */
    public void atualizarDados(String nome, Integer ano, LocalDate dataInicioInscricao,
                               LocalDate dataFimInscricao, LocalDate dataInicio, LocalDate dataFim) {
        if (this.status == StatusCompeticao.FINALIZADA || this.status == StatusCompeticao.CANCELADA) {
            throw new CompeticaoNaoEditavelException("Competição finalizada ou cancelada não pode ser editada");
        }
        validarPeriodo(dataInicioInscricao, dataFimInscricao);
        validarPeriodo(dataInicio, dataFim);
        this.nome = nome;
        this.ano = ano;
        this.dataInicioInscricao = dataInicioInscricao;
        this.dataFimInscricao = dataFimInscricao;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public void validar() {
        validarPeriodo(this.dataInicioInscricao, this.dataFimInscricao);
        validarPeriodo(this.dataInicio, this.dataFim);
    }

    void definirModalidade(Modalidade modalidade) {
        this.modalidade = modalidade;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = StatusCompeticao.PLANEJAMENTO;
        }
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && !dataFim.isAfter(dataInicio)) {
            throw new PeriodoInvalidoException("Data final deve ser posterior à data inicial");
        }
    }
}
