package br.com.gmontinny.batch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CnaeRow {
    private String secao;
    private String divisao;
    private String grupo;
    private String classe;
    private String subclasse;
    private String denominacao;
    private String observacoes;
}
