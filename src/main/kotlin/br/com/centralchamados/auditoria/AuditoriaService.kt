package br.com.centralchamados.auditoria

import br.com.centralchamados.auth.PrincipalUsuario
import br.com.centralchamados.chamados.model.Chamado
import br.com.centralchamados.chamados.repository.ChamadoRepository
import br.com.centralchamados.chamados.repository.acessoDoPerfil
import br.com.centralchamados.shared.api.ErroApi
import br.com.centralchamados.shared.model.Perfil
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuditoriaService(
    private val auditoria: AuditoriaRepository,
    private val chamados: ChamadoRepository,
) {
    fun registrar(
        chamadoId: UUID,
        tipoEvento: String,
        autorId: UUID,
        dados: Any,
    ) = auditoria.registrar(chamadoId, tipoEvento, autorId, dados)

    @Transactional(readOnly = true)
    fun listarDoChamado(
        principal: PrincipalUsuario,
        chamadoId: UUID,
    ): List<LogChamadoResposta> {
        val spec =
            Specification<Chamado> { root, query, cb ->
                val escopo = acessoDoPerfil(principal.id, principal.perfil, principal.setores).toPredicate(root, query, cb)
                cb.and(escopo ?: cb.conjunction(), cb.equal(root.get<UUID>("id"), chamadoId))
            }
        val chamado =
            chamados.findOne(spec).orElseThrow {
                ErroApi(HttpStatus.NOT_FOUND, "CHAMADO_NAO_ENCONTRADO", "Chamado não encontrado")
            }
        if (principal.perfil != Perfil.ADMIN && (principal.perfil != Perfil.ATENDENTE || chamado.setor.id !in principal.setores)) {
            throw ErroApi(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso negado")
        }
        return auditoria.listarPorChamado(chamadoId)
    }

    @Transactional(readOnly = true)
    fun listarAdmin(
        chamadoId: UUID?,
        autorId: UUID?,
        tipoEvento: String?,
        de: Instant?,
        ate: Instant?,
        pageable: Pageable,
    ): Page<LogChamadoResposta> = auditoria.listar(chamadoId, autorId, tipoEvento, de, ate, pageable)
}
