package br.com.gmontinny.mapper;

import br.com.gmontinny.domain.entity.Cnae;
import br.com.gmontinny.dto.response.CnaeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CnaeMapper {
    CnaeResponse toResponse(Cnae cnae);
    Cnae toEntity(CnaeResponse response);
}
