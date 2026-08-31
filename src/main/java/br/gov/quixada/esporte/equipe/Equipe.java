package br.gov.quixada.esporte.equipe;


import br.gov.quixada.esporte.clube.Clube;
import br.gov.quixada.esporte.modalidade.Modalidade;
import jakarta.persistence.*;

@Entity
@Table(name = "db_equipe")
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(optional = false)
    private Clube club;

    @ManyToOne(optional = false)
    private Modalidade modalidade;

    @Column(nullable = false)
    private String responsavel;

    @Column(nullable = false)
    private boolean ativo;
}
