package br.com.gmontinny.domain.repository;

import br.com.gmontinny.domain.entity.Cnae;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CnaeRepository extends JpaRepository<Cnae, Long> {
    Optional<Cnae> findBySubclasse(String subclasse);
    Page<Cnae> findByDenominacaoContainingIgnoreCase(String denominacao, Pageable pageable);
}
