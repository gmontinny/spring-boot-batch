package br.com.gmontinny.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

import java.net.URI;

@Slf4j
@Configuration
public class VaultConfig extends AbstractVaultConfiguration {

    @Value("${spring.cloud.vault.host:localhost}")
    private String vaultHost;

    @Value("${spring.cloud.vault.port:8200}")
    private int vaultPort;

    @Value("${spring.cloud.vault.scheme:http}")
    private String vaultScheme;

    @Value("${spring.cloud.vault.token:gmontinny-vault-token}")
    private String vaultToken;

    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = VaultEndpoint.from(
                URI.create(vaultScheme + "://" + vaultHost + ":" + vaultPort));
        log.info("[VAULT] Conectando em {}://{}:{}", vaultScheme, vaultHost, vaultPort);
        return endpoint;
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(vaultToken);
    }
}
