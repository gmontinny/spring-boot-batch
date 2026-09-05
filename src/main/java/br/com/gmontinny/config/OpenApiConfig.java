package br.com.gmontinny.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
}
