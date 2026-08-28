package br.gov.quixada.esporte.categoria;

import br.gov.quixada.esporte.competicao.Competicao;
import jakarta.persistence.*;

@Entity
@Table(name = "db_categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private Integer idadeMinima;

    private Integer idadeMaxima;

    @ManyToOne(optional = false)
    private Competicao competicao;
}
