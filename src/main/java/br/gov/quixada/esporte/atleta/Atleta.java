package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
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

    @Embedded
    private Endereco endereco;

    private String telefone;

    @Column(nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    private LocalDateTime dataCadastro;






}
