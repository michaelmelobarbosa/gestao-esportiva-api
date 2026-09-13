package br.gov.quixada.esporte.atleta;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.gov.quixada.esporte.extras.CpfUtils;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import br.gov.quixada.esporte.extras.StatusAtleta;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import br.gov.quixada.esporte.exceptions.AtletaInativoException;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_atletas")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Atleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nomeCompleto;

    @Column(unique = true, nullable = false, length = 11)
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter apenas números e ter 11 dígitos")
    private String cpf;

    @Column(nullable = false)
    @Past(message = "A data de nascimento deve ser no passado")
    private LocalDate dataNascimento;

    @Embedded
    @Valid
    @NotNull
    private Endereco endereco;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtleta status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sexo sexo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;


    public void ativar() {
        if (this.status == StatusAtleta.ATIVO) return;
        this.status = StatusAtleta.ATIVO;
    }

    public void inativar() {
        if (this.status == StatusAtleta.INATIVO) return;
        this.status = StatusAtleta.INATIVO;
    }

    /**
     * Atualiza dados mutáveis. CPF imutável após criação; bloqueia se INATIVO.
     */
    public void atualizarDados(String nomeCompleto, LocalDate dataNascimento, Endereco endereco, String telefone, Sexo sexo) {
        if (this.status == StatusAtleta.INATIVO) {
            throw new AtletaInativoException("Atleta está inativo, reative antes de editar");
        }
        this.nomeCompleto = nomeCompleto;
        this.dataNascimento = dataNascimento;
        this.endereco = endereco;
        this.telefone = telefone;
        this.sexo = sexo;
    }

    void definirCpfNormalizado(String cpf) {
        this.cpf = CpfUtils.normalize(cpf);
    }

    void definirStatus(StatusAtleta status) {
        this.status = status;
    }

    @PrePersist
    private void prePersist() {
        this.dataCadastro = LocalDateTime.now();
        if (this.cpf != null) {
            this.cpf = CpfUtils.normalize(this.cpf);
        }
        if (this.status == null) {
            this.status = StatusAtleta.ATIVO;
        }
    }

    @PreUpdate
    private void preUpdate() {
        if (this.cpf != null) {
            this.cpf = CpfUtils.normalize(this.cpf);
        }
    }
}
