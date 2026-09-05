package br.com.gmontinny.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "Resposta com dados do usuário")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse extends RepresentationModel<UserResponse> {

    @Schema(description = "ID do usuário")
    private Long id;

    @Schema(description = "Nome de usuário")
    private String username;

    @Schema(description = "E-mail do usuário")
    private String email;

    @Schema(description = "Status de ativação")
    private boolean enabled;

    @Schema(description = "Roles atribuídas")
    private Set<String> roles;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    public UserResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
