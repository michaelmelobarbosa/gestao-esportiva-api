package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "db_atletas")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Atleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nomeCompleto;

    @Column(unique = true, nullable = false,  length = 14)
    @CPF
    private String cpf;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    @Embedded
    @Column(nullable = false)
    private Endereco endereco;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sexo sexo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;






}
