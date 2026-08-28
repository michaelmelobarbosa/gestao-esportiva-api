package br.gov.quixada.esporte.modalidade;

import jakarta.persistence.*;

@Entity
@Table(name = "db_modalidade")
public class Modalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean ativo;
}
