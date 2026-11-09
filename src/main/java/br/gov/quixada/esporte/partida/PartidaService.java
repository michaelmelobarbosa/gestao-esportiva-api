package br.gov.quixada.esporte.partida;

import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.inscricao.InscricaoEquipe;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeRepository;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.partida.dto.PartidaCreateRequest;
import br.gov.quixada.esporte.partida.dto.PartidaResultadoRequest;
import br.gov.quixada.esporte.partida.dto.PartidaUpdateRequest;
import br.gov.quixada.esporte.partida.exception.PartidaInvalidaException;
import br.gov.quixada.esporte.partida.exception.PartidaNotFoundException;
import br.gov.quixada.esporte.partida.exception.TransicaoStatusPartidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartidaService {

    private static final List<StatusPartida> STATUS_CANCELAVEIS =
            List.of(StatusPartida.AGENDADA, StatusPartida.EM_ANDAMENTO);

    private final PartidaRepository repository;
    private final InscricaoEquipeRepository inscricaoEquipeRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<Partida> findAll(Long competicaoId, Long categoriaId, StatusPartida status, Pageable pageable) {
        return repository.search(competicaoId, categoriaId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Partida findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PartidaNotFoundException("Partida não encontrada"));
    }

    @Transactional
    public Partida create(PartidaCreateRequest request) {
        var mandante = buscarInscricao(request.inscricaoMandanteId());
        var visitante = buscarInscricao(request.inscricaoVisitanteId());
        validarDados(request.rodada(), request.dataHora(), mandante, visitante, -1L);

        var statusInicial = mandante.getCompeticao().calcularStatus(LocalDate.now(clock))
                == StatusCompeticao.CANCELADA
                ? StatusPartida.CANCELADA
                : StatusPartida.AGENDADA;

        var partida = Partida.builder()
                .rodada(request.rodada())
                .dataHora(request.dataHora())
                .local(request.local())
                .inscricaoMandante(mandante)
                .inscricaoVisitante(visitante)
                .status(statusInicial)
                .build();
        return repository.save(partida);
    }

    @Transactional
    public Partida update(Long id, PartidaUpdateRequest request) {
        var partida = findByIdOrThrowNotFound(id);
        if (partida.getStatus() != StatusPartida.AGENDADA) {
            throw new PartidaInvalidaException("Só é possível editar partidas com status AGENDADA");
        }

        var mandante = buscarInscricao(request.inscricaoMandanteId());
        var visitante = buscarInscricao(request.inscricaoVisitanteId());
        validarDados(request.rodada(), request.dataHora(), mandante, visitante, id);

        partida.atualizarDados(request.rodada(), request.dataHora(), request.local(), mandante, visitante);
        return partida;
    }

    @Transactional
    public Partida alterarStatus(Long id, StatusPartida status) {
        var partida = findByIdOrThrowNotFound(id);

        if (status == StatusPartida.FINALIZADA) {
            throw new PartidaInvalidaException("Use o endpoint de resultado para finalizar a partida");
        }
        if (!partida.getStatus().podeTransicionarPara(status)) {
            throw new TransicaoStatusPartidaException(
                    "Transição de status inválida: " + partida.getStatus() + " -> " + status);
        }

        partida.alterarStatus(status);
        return partida;
    }

    @Transactional
    public Partida registrarResultado(Long id, PartidaResultadoRequest request) {
        var partida = findByIdOrThrowNotFound(id);
        if (partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            throw new PartidaInvalidaException("Só é possível registrar resultado de partida EM_ANDAMENTO");
        }
        if (request.golsMandante() == null || request.golsVisitante() == null
                || request.golsMandante() < 0 || request.golsVisitante() < 0) {
            throw new PartidaInvalidaException("Gols não podem ser nulos ou negativos");
        }

        partida.registrarResultado(request.golsMandante(), request.golsVisitante());
        return partida;
    }

    @Transactional
    public void cancelarPorCompeticao(Long competicaoId) {
        repository.cancelarPorCompeticao(competicaoId, STATUS_CANCELAVEIS, StatusPartida.CANCELADA);
    }

    @Transactional
    public void cancelarPorInscricao(Long inscricaoId) {
        repository.cancelarPorInscricao(inscricaoId, STATUS_CANCELAVEIS, StatusPartida.CANCELADA);
    }

    private void validarDados(Integer rodada, java.time.LocalDateTime dataHora,
                              InscricaoEquipe mandante, InscricaoEquipe visitante, Long idIgnorado) {
        if (mandante.getId().equals(visitante.getId())) {
            throw new PartidaInvalidaException("Mandante e visitante devem ser diferentes");
        }
        if (mandante.getStatus() != StatusInscricao.APROVADA || visitante.getStatus() != StatusInscricao.APROVADA) {
            throw new PartidaInvalidaException("As duas equipes precisam ter inscrição APROVADA");
        }
        if (!mandante.getCompeticao().getId().equals(visitante.getCompeticao().getId())) {
            throw new PartidaInvalidaException("As equipes pertencem a competições diferentes");
        }
        if (!mandante.getCategoria().getId().equals(visitante.getCategoria().getId())) {
            throw new PartidaInvalidaException("As equipes pertencem a categorias diferentes");
        }

        Competicao competicao = mandante.getCompeticao();
        LocalDate dataPartida = dataHora.toLocalDate();
        if (dataPartida.isBefore(competicao.getDataInicio()) || dataPartida.isAfter(competicao.getDataFim())) {
            throw new PartidaInvalidaException("Data da partida deve estar dentro do período da competição");
        }

        if (repository.existsConfrontoNaRodada(mandante.getId(), visitante.getId(), rodada,
                idIgnorado, StatusPartida.CANCELADA)) {
            throw new PartidaInvalidaException("Já existe uma partida entre essas equipes nesta rodada");
        }

        if (repository.existsEquipeNaRodada(List.of(mandante.getId(), visitante.getId()), rodada,
                idIgnorado, StatusPartida.CANCELADA)) {
            throw new PartidaInvalidaException("Uma das equipes já tem partida nesta rodada");
        }

        if (repository.existsConflitoDataHora(List.of(mandante.getId(), visitante.getId()), dataHora,
                idIgnorado, StatusPartida.CANCELADA)) {
            throw new PartidaInvalidaException("Uma das equipes já tem partida neste horário");
        }
    }

    private InscricaoEquipe buscarInscricao(Long id) {
        return inscricaoEquipeRepository.findById(id)
                .orElseThrow(() -> new InscricaoEquipeNotFoundException("Inscrição de equipe não encontrada"));
    }
}
