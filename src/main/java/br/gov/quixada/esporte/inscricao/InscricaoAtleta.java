package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.atleta.Atleta;
import br.gov.quixada.esporte.extras.StatusInscricao;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "db_inscricao_atleta")
public class InscricaoAtleta {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @ManyToOne()
    private Atleta atleta;

    @ManyToOne()
    private InscricaoEquipe inscricaoEquipe;

    private String numeroCamisa;

    private LocalDateTime dataInscricao;

    private StatusInscricao status;


}
