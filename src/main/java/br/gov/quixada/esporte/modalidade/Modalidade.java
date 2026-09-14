package br.gov.quixada.esporte.modalidade;

import br.gov.quixada.esporte.modalidade.exception.ModalidadeInativaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "db_modalidade")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Modalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusModalidade status;

    public void ativar() {
        if (this.status == StatusModalidade.ATIVO) return;
        this.status = StatusModalidade.ATIVO;
    }

    public void inativar() {
        if (this.status == StatusModalidade.INATIVO) return;
        this.status = StatusModalidade.INATIVO;
    }

    /** Atualiza dados mutáveis. Bloqueia edição se INATIVO. */
    public void atualizarDados(String nome) {
        if (this.status == StatusModalidade.INATIVO) {
            throw new ModalidadeInativaException("Modalidade está inativa, reative antes de editar");
        }
        this.nome = nome;
    }

    void definirStatus(StatusModalidade status) {
        this.status = status;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = StatusModalidade.ATIVO;
        }
    }
}
