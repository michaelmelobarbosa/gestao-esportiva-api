package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "db_atletas")
public class Atleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nomeCompleto;

    @Column(unique = true, nullable = false)
    private String cpf;

    @Column(nullable = false)
    private LocalDateTime dataNascimento;

    @Embedded
    private Endereco endereco;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @Column(nullable = false)
    private LocalDateTime dataCadastro;






}
