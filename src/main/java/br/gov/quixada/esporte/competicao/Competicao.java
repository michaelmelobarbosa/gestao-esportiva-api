package br.gov.quixada.esporte.competicao;

import br.gov.quixada.esporte.extras.StatusCompeticao;
import br.gov.quixada.esporte.modalidade.Modalidade;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "db_competicao")
public class Competicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer ano;

    @ManyToOne(optional = false)
    private Modalidade modalidade;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime dataInicio;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime dataFim;

    @Enumerated(EnumType.STRING)
    private StatusCompeticao status;
}
