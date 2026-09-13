package br.gov.quixada.esporte.clube;

import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.StatusClube;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_clube")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Clube {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 100)
    private String responsavel;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Embedded
    @Valid
    @NotNull
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusClube status;

    /** Atualiza dados mutáveis. */
    public void atualizarDados(String nome, String responsavel, String telefone, Endereco endereco) {
        this.nome = nome;
        this.responsavel = responsavel;
        this.telefone = telefone;
        this.endereco = endereco;
    }

    public void ativar() {
        if (this.status == StatusClube.ATIVO) return;
        this.status = StatusClube.ATIVO;
    }

    public void inativar() {
        if (this.status == StatusClube.INATIVO) return;
        this.status = StatusClube.INATIVO;
    }
}
