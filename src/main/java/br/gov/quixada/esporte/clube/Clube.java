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
    private String reponsavel;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private Endereco endereco;

    @Column(nullable = false)
    private boolean ativo;

}
