package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.atleta.Atleta;
import br.gov.quixada.esporte.extras.StatusInscricao;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "db_inscricao_atleta")
public class InscricaoAtleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @OneToOne(cascade = CascadeType.ALL)
    private Atleta atleta;

    @ManyToOne()
    private InscricaoEquipe inscricaoEquipe;

    private String numeroCamisa;

    private LocalDateTime dataInscricao;

    @Enumerated(EnumType.STRING)
    private StatusInscricao status;


}
