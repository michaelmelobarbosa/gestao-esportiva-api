package br.gov.quixada.sports.modality;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModalityRepository extends JpaRepository<Modality, Long> {

    Page<Modality> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Modality> findByStatus(ModalityStatus status, Pageable pageable);

    Page<Modality> findByNameContainingIgnoreCaseAndStatus(String name, ModalityStatus status, Pageable pageable);
}
