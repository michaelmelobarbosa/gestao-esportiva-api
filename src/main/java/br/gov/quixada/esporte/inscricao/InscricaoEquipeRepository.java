package br.gov.quixada.esporte.inscricao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InscricaoEquipeRepository extends JpaRepository<InscricaoEquipe, Long> {
}
