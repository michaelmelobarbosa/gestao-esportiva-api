package br.gov.quixada.esporte.atleta;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "db_atletas")
public class Atleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(unique = true, nullable = false)
    private String nomeCompleto;

    @Column(unique = true, nullable = false)
    private String cpf;

    @Column(nullable = false)
    private LocalDateTime dataNascimento;

    private String telefone;

    @Column(nullable = false)
    private boolean ativo;

    private LocalDateTime dataCadastro;






}
