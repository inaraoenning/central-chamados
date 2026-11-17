package br.com.centralchamados.chamados.repository

import br.com.centralchamados.chamados.model.Chamado
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ChamadoRepository :
    JpaRepository<Chamado, UUID>,
    JpaSpecificationExecutor<Chamado> {
    @Query("select c from Chamado c where c.solicitante.id = :id")
    fun meus(
        @Param("id") id: UUID,
        pageable: Pageable,
    ): Page<Chamado>

    @Query("select c from Chamado c where c.solicitante.id = :id and c.id = :chamadoId")
    fun meuPorId(
        @Param("id") id: UUID,
        @Param("chamadoId") chamadoId: UUID,
    ): Chamado?

    @Query(
        "select c from Chamado c join c.setor s where s.id in :setorIds and c.status <> br.com.centralchamados.shared.model.StatusChamado.CONCLUIDO",
    )
    fun fila(
        @Param("setorIds") setorIds: Collection<UUID>,
        pageable: Pageable,
    ): Page<Chamado>

    @Query(
        value = """select c from Chamado c join c.setor s where s.id in :setorIds
            and c.status <> br.com.centralchamados.shared.model.StatusChamado.CONCLUIDO
            order by case when c.status = br.com.centralchamados.shared.model.StatusChamado.ABERTO then 0 else 1 end,
            case c.prioridade when br.com.centralchamados.shared.model.Prioridade.CRITICA then 0
                when br.com.centralchamados.shared.model.Prioridade.ALTA then 1
                when br.com.centralchamados.shared.model.Prioridade.MEDIA then 2 else 3 end, c.criadoEm asc""",
        countQuery = """select count(c) from Chamado c join c.setor s where s.id in :setorIds
            and c.status <> br.com.centralchamados.shared.model.StatusChamado.CONCLUIDO""",
    )
    fun filaOrdenada(
        @Param("setorIds") setorIds: Collection<UUID>,
        pageable: Pageable,
    ): Page<Chamado>

    @Query("select c from Chamado c where c.status = br.com.centralchamados.shared.model.StatusChamado.ABERTO and c.criadoEm < :limite")
    fun chamadosSemAtendimento(
        @Param("limite") limite: Instant,
    ): List<Chamado>

    @Query(
        "select c from Chamado c where c.status = br.com.centralchamados.shared.model.StatusChamado.EM_ANDAMENTO and c.previsaoResolucao is not null and c.previsaoResolucao between :inicio and :fim",
    )
    fun previsoesProximas(
        @Param("inicio") inicio: Instant,
        @Param("fim") fim: Instant,
    ): List<Chamado>

    @Query(
        "select c from Chamado c where c.status = br.com.centralchamados.shared.model.StatusChamado.EM_ANDAMENTO and c.previsaoResolucao is not null and c.previsaoResolucao < :agora",
    )
    fun previsoesVencidas(
        @Param("agora") agora: Instant,
    ): List<Chamado>
}

fun acessoDoPerfil(
    principalId: UUID,
    perfil: br.com.centralchamados.shared.model.Perfil,
    setorIds: Set<UUID>,
): Specification<Chamado> =
    Specification { root, _, cb ->
        when (perfil) {
            br.com.centralchamados.shared.model.Perfil.SOLICITANTE ->
                cb.equal(root.get<br.com.centralchamados.usuarios.model.Usuario>("solicitante").get<UUID>("id"), principalId)
            br.com.centralchamados.shared.model.Perfil.ATENDENTE ->
                root.get<br.com.centralchamados.setores.model.Setor>("setor").get<UUID>("id").`in`(setorIds)
            br.com.centralchamados.shared.model.Perfil.ADMIN -> cb.conjunction()
        }
    }
