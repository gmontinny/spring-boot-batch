package br.com.gmontinny.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Schema(description = "Resposta com dados de CNAE")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CnaeResponse extends RepresentationModel<CnaeResponse> {

    @Schema(description = "ID do registro")
    private Long id;

    @Schema(description = "Seção CNAE", example = "A")
    private String secao;

    @Schema(description = "Divisão CNAE", example = "01")
    private String divisao;

    @Schema(description = "Grupo CNAE", example = "01.1")
    private String grupo;

    @Schema(description = "Classe CNAE", example = "01.11")
    private String classe;

    @Schema(description = "Subclasse CNAE", example = "0111-3/01")
    private String subclasse;

    @Schema(description = "Denominação da atividade")
    private String denominacao;

    @Schema(description = "Observações adicionais")
    private String observacoes;

    @Schema(description = "Data de processamento")
    private LocalDateTime processedAt;

    public CnaeResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSecao() { return secao; }
    public void setSecao(String secao) { this.secao = secao; }
    public String getDivisao() { return divisao; }
    public void setDivisao(String divisao) { this.divisao = divisao; }
    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public String getClasse() { return classe; }
    public void setClasse(String classe) { this.classe = classe; }
    public String getSubclasse() { return subclasse; }
    public void setSubclasse(String subclasse) { this.subclasse = subclasse; }
    public String getDenominacao() { return denominacao; }
    public void setDenominacao(String denominacao) { this.denominacao = denominacao; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
