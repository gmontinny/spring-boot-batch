package br.com.gmontinny.service;

import br.com.gmontinny.domain.entity.Cnae;
import br.com.gmontinny.domain.repository.CnaeRepository;
import br.com.gmontinny.dto.response.CnaeResponse;
import br.com.gmontinny.mapper.CnaeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CnaeService — Testes Unitários")
class CnaeServiceTest {

    @Mock private CnaeRepository cnaeRepository;
    @Mock private CnaeMapper cnaeMapper;

    @InjectMocks
    private CnaeService cnaeService;

    @Test
    @DisplayName("Deve retornar página de CNAEs")
    void shouldReturnPagedCnaes() {
        Cnae cnae = cnae("Cultivo de arroz");
        CnaeResponse response = new CnaeResponse();
        response.setDenominacao("Cultivo de arroz");
        PageRequest pageable = PageRequest.of(0, 10);

        when(cnaeRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(cnae)));
        when(cnaeMapper.toResponse(cnae)).thenReturn(response);

        Page<CnaeResponse> result = cnaeService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDenominacao()).isEqualTo("Cultivo de arroz");
    }

    @Test
    @DisplayName("Deve retornar CNAE por ID")
    void shouldFindById() {
        Cnae cnae = cnae("Criação de bovinos");
        CnaeResponse response = new CnaeResponse();
        response.setId(1L);

        when(cnaeRepository.findById(1L)).thenReturn(Optional.of(cnae));
        when(cnaeMapper.toResponse(cnae)).thenReturn(response);

        CnaeResponse result = cnaeService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lançar 404 quando CNAE não encontrado")
    void shouldThrowNotFoundForUnknownId() {
        when(cnaeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cnaeService.findById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Deve pesquisar CNAEs por denominação")
    void shouldSearchByDenominacao() {
        Cnae cnae = cnae("Agricultura familiar");
        CnaeResponse response = new CnaeResponse();
        response.setDenominacao("Agricultura familiar");
        PageRequest pageable = PageRequest.of(0, 10);

        when(cnaeRepository.findByDenominacaoContainingIgnoreCase("agricultura", pageable))
                .thenReturn(new PageImpl<>(List.of(cnae)));
        when(cnaeMapper.toResponse(cnae)).thenReturn(response);

        Page<CnaeResponse> result = cnaeService.search("agricultura", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDenominacao()).containsIgnoringCase("agricultura");
    }

    private Cnae cnae(String denominacao) {
        Cnae c = new Cnae();
        c.setDenominacao(denominacao);
        return c;
    }
}
