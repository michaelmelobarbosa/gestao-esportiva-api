package br.gov.quixada.esporte.partida;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.categoria.CategoriaRepository;
import br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.inscricao.InscricaoEquipe;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeRepository;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException;
import br.gov.quixada.esporte.partida.dto.ClassificacaoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassificacaoService {

    private final PartidaRepository repository;
    private final CompeticaoRepository competicaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InscricaoEquipeRepository inscricaoEquipeRepository;

    @Transactional(readOnly = true)
    public List<ClassificacaoResponse> calcular(Long competicaoId, Long categoriaId) {
        Competicao competicao = competicaoRepository.findById(competicaoId)
                .orElseThrow(() -> new CompeticaoNotFoundException("Competição não encontrada"));
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new CategoriaNotFoundException("Categoria não encontrada"));

        if (!categoria.getCompeticao().getId().equals(competicao.getId())) {
            throw new InscricaoInvalidaException("Categoria não pertence à competição informada");
        }

        Map<Long, Linha> linhas = new LinkedHashMap<>();
        inscricaoEquipeRepository.findByCompeticaoIdAndCategoriaIdAndStatus(
                        competicaoId, categoriaId, StatusInscricao.APROVADA)
                .forEach(inscricao -> linhas.put(inscricao.getId(), new Linha(inscricao)));

        List<Partida> partidas = repository.findByCompeticaoAndCategoria(competicaoId, categoriaId);
        for (Partida partida : partidas) {
            if (!partidaValidaParaClassificacao(partida)) {
                continue;
            }

            InscricaoEquipe mandante = partida.getInscricaoMandante();
            InscricaoEquipe visitante = partida.getInscricaoVisitante();
            Linha linhaMandante = linhas.computeIfAbsent(mandante.getId(), id -> new Linha(mandante));
            Linha linhaVisitante = linhas.computeIfAbsent(visitante.getId(), id -> new Linha(visitante));

            int golsMandante = partida.getGolsMandante();
            int golsVisitante = partida.getGolsVisitante();

            linhaMandante.jogos++;
            linhaVisitante.jogos++;
            linhaMandante.golsPro += golsMandante;
            linhaMandante.golsContra += golsVisitante;
            linhaVisitante.golsPro += golsVisitante;
            linhaVisitante.golsContra += golsMandante;

            if (golsMandante > golsVisitante) {
                linhaMandante.vitorias++;
                linhaVisitante.derrotas++;
            } else if (golsMandante < golsVisitante) {
                linhaVisitante.vitorias++;
                linhaMandante.derrotas++;
            } else {
                linhaMandante.empates++;
                linhaVisitante.empates++;
            }
        }

        List<Linha> ordenadas = new ArrayList<>(linhas.values());
        ordenadas.sort((a, b) -> comparar(a, b, partidas));

        List<ClassificacaoResponse> resposta = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            Linha linha = ordenadas.get(i);
            resposta.add(new ClassificacaoResponse(
                    i + 1,
                    linha.inscricao.getId(),
                    linha.inscricao.getEquipe().getId(),
                    linha.inscricao.getEquipe().getNome(),
                    linha.pontos(),
                    linha.jogos,
                    linha.vitorias,
                    linha.empates,
                    linha.derrotas,
                    linha.golsPro,
                    linha.golsContra,
                    linha.saldo(),
                    linha.aproveitamento()
            ));
        }
        return resposta;
    }

    private boolean partidaValidaParaClassificacao(Partida partida) {
        return partida.getStatus() == StatusPartida.FINALIZADA
                && partida.getGolsMandante() != null
                && partida.getGolsVisitante() != null;
    }

    private int comparar(Linha a, Linha b, List<Partida> partidas) {
        int comparacao = Integer.compare(b.pontos(), a.pontos());
        if (comparacao != 0) return comparacao;

        comparacao = Integer.compare(b.vitorias, a.vitorias);
        if (comparacao != 0) return comparacao;

        comparacao = Integer.compare(b.saldo(), a.saldo());
        if (comparacao != 0) return comparacao;

        comparacao = Integer.compare(b.golsPro, a.golsPro);
        if (comparacao != 0) return comparacao;

        comparacao = confrontoDireto(a, b, partidas);
        if (comparacao != 0) return comparacao;

        return a.inscricao.getEquipe().getNome().compareToIgnoreCase(b.inscricao.getEquipe().getNome());
    }

    private int confrontoDireto(Linha a, Linha b, List<Partida> partidas) {
        int pontosA = 0;
        int pontosB = 0;
        int saldoA = 0;
        int saldoB = 0;

        for (Partida partida : partidas) {
            if (!partidaValidaParaClassificacao(partida)) continue;

            boolean aMandante = partida.getInscricaoMandante().getId().equals(a.inscricao.getId());
            boolean bMandante = partida.getInscricaoMandante().getId().equals(b.inscricao.getId());
            if (!(aMandante && partida.getInscricaoVisitante().getId().equals(b.inscricao.getId()))
                    && !(bMandante && partida.getInscricaoVisitante().getId().equals(a.inscricao.getId()))) {
                continue;
            }

            int golsA = aMandante ? partida.getGolsMandante() : partida.getGolsVisitante();
            int golsB = aMandante ? partida.getGolsVisitante() : partida.getGolsMandante();
            saldoA += golsA - golsB;
            saldoB += golsB - golsA;

            if (golsA > golsB) {
                pontosA += 3;
            } else if (golsB > golsA) {
                pontosB += 3;
            } else {
                pontosA++;
                pontosB++;
            }
        }

        int comparacao = Integer.compare(pontosB, pontosA);
        if (comparacao != 0) return comparacao;
        return Integer.compare(saldoB, saldoA);
    }

    private static final class Linha {
        private final InscricaoEquipe inscricao;
        private int jogos;
        private int vitorias;
        private int empates;
        private int derrotas;
        private int golsPro;
        private int golsContra;

        private Linha(InscricaoEquipe inscricao) {
            this.inscricao = inscricao;
        }

        private int pontos() {
            return vitorias * 3 + empates;
        }

        private int saldo() {
            return golsPro - golsContra;
        }

        private double aproveitamento() {
            if (jogos == 0) return 0.0;
            double valor = (pontos() / (jogos * 3.0)) * 100.0;
            return Math.round(valor * 100.0) / 100.0;
        }
    }
}
