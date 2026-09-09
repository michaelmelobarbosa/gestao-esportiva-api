package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.extras.CpfUtils;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import br.gov.quixada.esporte.extras.StatusAtleta;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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

    @Column(unique = true, nullable = false,  length = 11)
    @CPF(message = "CPF inválido")
    @Pattern (regexp = "\\d{11}", message = "CPF deve conter apenas números e ter 11 dígitos")
    private String cpf;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    @Embedded
    @Valid
    private Endereco endereco;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtleta status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sexo sexo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist 
    @PreUpdate
    public void setCpf(String cpf) {
        this.cpf = CpfUtils.normalize(cpf);
    }



    


}
