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

    @Column(nullable = false, length = 100)
    private String nome;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clube")
    private Clube clube;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modalidade")
    private Modalidade modalidade;

    @Column(nullable = false, length = 100)
    private String responsavel;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean ativo;
}
