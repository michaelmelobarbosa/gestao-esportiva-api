package br.gov.quixada.esporte.clube;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClubeRepository extends JpaRepository<Clube, Long> {

    Page<Clube> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
