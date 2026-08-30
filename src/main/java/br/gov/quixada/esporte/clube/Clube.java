package br.gov.quixada.esporte.clube;

import br.gov.quixada.esporte.extras.Endereco;
import jakarta.persistence.*;

@Entity
@Table(name = "db_clube")
public class Clube {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String responsavel;

    @Column(nullable = false)
    private String telefone;

    @Embedded
    private Endereco endereco;

    @Column(nullable = false)
    private boolean ativo;

}
