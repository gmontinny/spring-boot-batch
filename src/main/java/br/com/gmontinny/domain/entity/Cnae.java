package br.com.gmontinny.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cnae")
@Getter @Setter @NoArgsConstructor
public class Cnae {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10)
    private String secao;

    @Column(length = 10)
    private String divisao;

    @Column(length = 10)
    private String grupo;

    @Column(length = 10)
    private String classe;

    @Column(length = 20)
    private String subclasse;

    @Column(nullable = false, length = 500)
    private String denominacao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();
}
