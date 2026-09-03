package br.gov.quixada.esporte.competicao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompeticaoRepository extends JpaRepository<Competicao, Long> {
}
