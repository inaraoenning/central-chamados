package br.com.centralchamados.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CredenciaisLogin(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val senha: String,
)
