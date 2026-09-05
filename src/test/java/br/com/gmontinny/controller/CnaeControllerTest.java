package br.com.gmontinny.controller;

import br.com.gmontinny.dto.response.CnaeResponse;
import br.com.gmontinny.service.CnaeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CnaeController — Testes Unitários")
class CnaeControllerTest {

    @Mock private CnaeService cnaeService;
    @Mock private PagedResourcesAssembler<CnaeResponse> assembler;
    @InjectMocks private CnaeController cnaeController;

    @Test
    @DisplayName("findAll — deve usar sort=id quando campo inválido é informado")
    void shouldFallbackToIdWhenSortInvalid() {
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        when(cnaeService.findAll(captor.capture())).thenReturn(new PageImpl<>(List.of()));
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        cnaeController.findAll(0, 20, "[\"denominacao\"]", "asc");

        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("[\"denominacao\"]")).isNull();
    }

    @Test
    @DisplayName("findAll — deve usar sort=denominacao quando campo válido é informado")
    void shouldUseValidSortField() {
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        when(cnaeService.findAll(captor.capture())).thenReturn(new PageImpl<>(List.of()));
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        cnaeController.findAll(0, 20, "denominacao", "desc");

        Sort.Order order = captor.getValue().getSort().getOrderFor("denominacao");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("findAll — deve retornar 200")
    void shouldReturn200() {
        when(cnaeService.findAll(any())).thenReturn(new PageImpl<>(List.of()));
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        ResponseEntity<?> result = cnaeController.findAll(0, 20, "id", "asc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("search — deve usar sort=id quando campo inválido é informado")
    void shouldFallbackToIdOnSearchWithInvalidSort() {
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        when(cnaeService.search(eq("agricultura"), captor.capture())).thenReturn(new PageImpl<>(List.of()));
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        cnaeController.search("agricultura", 0, 20, "[\"subclasse\"]", "asc");

        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    @DisplayName("search — deve retornar 200")
    void shouldReturn200OnSearch() {
        when(cnaeService.search(eq("arroz"), any())).thenReturn(new PageImpl<>(List.of()));
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        ResponseEntity<?> result = cnaeController.search("arroz", 0, 20, "id", "asc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
