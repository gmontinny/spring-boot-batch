package br.com.gmontinny.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GMontinny API")
                        .description("""
                                API REST para processamento de dados CNAE via Spring Batch.
                                
                                **Funcionalidades:**
                                - Autenticação JWT com controle de acesso por roles (ADMIN / USER)
                                - Processamento de planilhas Excel via Spring Batch integrado ao RabbitMQ
                                - CRUD de usuários e consulta de CNAEs com HATEOAS
                                
                                **Como autenticar:**
                                1. Faça POST em `/api/v1/auth/login` com username e password
                                2. Copie o token retornado
                                3. Clique em **Authorize** e informe: `Bearer {token}`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GMontinny")
                                .email("admin@gmontinny.com.br"))
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Informe o token JWT obtido no endpoint /api/v1/auth/login")));
    }

    /**
     * Corrige o parâmetro 'sort' do Pageable no Swagger UI.
     * Por padrão o SpringDoc gera sort como array, mas o Spring Data JPA
     * espera strings simples como: sort=username,asc
     */
    @Bean
    public OperationCustomizer pageableSortFix() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() == null) return operation;
            operation.getParameters().forEach(param -> {
                if ("sort".equals(param.getName())) {
                    param.setDescription("Campo e direção de ordenação. Exemplo: `username,asc` ou `createdAt,desc`");
                    param.schema(new StringSchema().example("username,asc"));
                }
            });
            return operation;
        };
    }
}
