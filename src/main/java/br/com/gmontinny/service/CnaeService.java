package br.com.gmontinny.service;

import br.com.gmontinny.domain.repository.CnaeRepository;
import br.com.gmontinny.dto.response.CnaeResponse;
import br.com.gmontinny.mapper.CnaeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CnaeService {

    private final CnaeRepository cnaeRepository;
    private final CnaeMapper cnaeMapper;

    @Transactional(readOnly = true)
    public Page<CnaeResponse> findAll(Pageable pageable) {
        return cnaeRepository.findAll(pageable).map(cnaeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CnaeResponse findById(Long id) {
        return cnaeRepository.findById(id)
                .map(cnaeMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CNAE não encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<CnaeResponse> search(String denominacao, Pageable pageable) {
        return cnaeRepository.findByDenominacaoContainingIgnoreCase(denominacao, pageable)
                .map(cnaeMapper::toResponse);
    }
}
