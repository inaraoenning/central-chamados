package br.com.centralchamados.usuarios

import br.com.centralchamados.provedores.repository.ProvedorRepository
import br.com.centralchamados.setores.repository.SetorRepository
import br.com.centralchamados.shared.api.ErroApi
import br.com.centralchamados.shared.model.Perfil
import br.com.centralchamados.usuarios.model.Usuario
import br.com.centralchamados.usuarios.repository.UsuarioRepository
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UsuarioService(
    private val usuarios: UsuarioRepository,
    private val provedores: ProvedorRepository,
    private val setores: SetorRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional(readOnly = true)
    fun listar(pageable: Pageable) = usuarios.findAll(pageable).map { it.toResposta() }

    @Transactional
    fun criar(body: UsuarioRequest): UsuarioAdminResposta {
        if (usuarios.findByEmailIgnoreCase(body.email) != null) {
            throw ErroApi(HttpStatus.CONFLICT, "EMAIL_EM_USO", "E-mail já cadastrado")
        }
        validarPerfil(body)
        val provedor = body.provedorId?.let { provedores.findById(it).orElseThrow { naoEncontrado("Provedor") } }
        val setoresUsuario = setores.findAllById(body.setoresIds).toMutableSet()
        if (setoresUsuario.size != body.setoresIds.size) throw naoEncontrado("Setor")
        return usuarios
            .save(
                Usuario(
                    nome = body.nome,
                    email = body.email.lowercase(),
                    senhaHash = passwordEncoder.encode(body.senha),
                    perfil = body.perfil!!,
                    provedor = provedor,
                    setores = setoresUsuario,
                ),
            ).toResposta()
    }

    @Transactional
    fun atualizar(
        id: UUID,
        body: UsuarioRequest,
    ): UsuarioAdminResposta {
        val usuario = usuarios.findById(id).orElseThrow { naoEncontrado("Usuário") }
        val existente = usuarios.findByEmailIgnoreCase(body.email)
        if (existente != null && existente.id != id) throw ErroApi(HttpStatus.CONFLICT, "EMAIL_EM_USO", "E-mail já cadastrado")
        validarPerfil(body)
        usuario.nome = body.nome
        usuario.email = body.email.lowercase()
        usuario.senhaHash = passwordEncoder.encode(body.senha)
        usuario.perfil = body.perfil!!
        usuario.provedor = body.provedorId?.let { provedores.findById(it).orElseThrow { naoEncontrado("Provedor") } }
        usuario.setores = setores.findAllById(body.setoresIds).toMutableSet()
        if (usuario.setores.size != body.setoresIds.size) throw naoEncontrado("Setor")
        return usuarios.save(usuario).toResposta()
    }

    @Transactional
    fun desativar(id: UUID) {
        usuarios.findById(id).orElseThrow { naoEncontrado("Usuário") }.ativo = false
    }

    private fun Usuario.toResposta() = UsuarioAdminResposta(id, nome, email, perfil, provedor?.id, setores.map { it.id }.toSet(), ativo)

    private fun validarPerfil(body: UsuarioRequest) {
        if (body.perfil == Perfil.SOLICITANTE && body.provedorId == null) {
            throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "PROVEDOR_OBRIGATORIO", "Solicitante deve pertencer a um provedor")
        }
        if (body.perfil != Perfil.SOLICITANTE && body.provedorId != null) {
            throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "PROVEDOR_INVALIDO", "Equipe interna não pode pertencer a um provedor")
        }
        if (body.perfil == Perfil.ATENDENTE && body.setoresIds.isEmpty()) {
            throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "SETOR_OBRIGATORIO", "Atendente deve pertencer a pelo menos um setor")
        }
    }

    private fun naoEncontrado(recurso: String) = ErroApi(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "$recurso não encontrado")
}
