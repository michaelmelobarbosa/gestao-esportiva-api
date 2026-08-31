package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.atleta.Atleta;
import br.gov.quixada.esporte.extras.StatusInscricao;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "db_inscricao_atleta")
public class InscricaoAtleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @ManyToOne(optional = false)
    private Atleta atleta;

    @ManyToOne(optional = false)
    private InscricaoEquipe inscricaoEquipe;

    @Column(nullable = false)
    private String numeroCamisa;

    @CreationTimestamp
    private LocalDateTime dataInscricao;

    @Enumerated(EnumType.STRING)
    private StatusInscricao status;


}
