package br.com.centralchamados.notificacoes

import br.com.centralchamados.notificacoes.model.DispositivoPush
import br.com.centralchamados.notificacoes.repository.DispositivoPushRepository
import br.com.centralchamados.usuarios.repository.UsuarioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DispositivoPushService(
    private val usuarios: UsuarioRepository,
    private val dispositivos: DispositivoPushRepository,
) {
    @Transactional
    fun registrar(
        usuarioId: UUID,
        body: DispositivoRequest,
    ) {
        val usuario = usuarios.findById(usuarioId).orElseThrow()
        val dispositivo =
            dispositivos.findByTokenAndUsuarioId(body.token, usuarioId)
                ?: DispositivoPush(usuario = usuario, token = body.token)
        dispositivo.plataforma = body.plataforma
        dispositivo.ativo = true
        dispositivos.save(dispositivo)
    }

    @Transactional
    fun remover(
        usuarioId: UUID,
        token: String,
    ) = dispositivos.deleteByTokenAndUsuarioId(token, usuarioId)
}
