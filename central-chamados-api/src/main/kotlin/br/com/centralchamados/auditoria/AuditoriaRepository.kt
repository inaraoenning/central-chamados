package br.com.centralchamados.auditoria

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class AuditoriaRepository(
    private val jdbc: JdbcTemplate,
    private val mapper: ObjectMapper,
) {
    fun registrar(
        chamadoId: UUID,
        tipoEvento: String,
        autorId: UUID,
        dados: Any,
    ) {
        jdbc.update(
            "INSERT INTO chamado_log(chamado_id, tipo_evento, autor_id, dados, criado_em) VALUES (?, ?, ?, ?::jsonb, ?)",
            chamadoId,
            tipoEvento,
            autorId,
            mapper.writeValueAsString(dados),
            Timestamp.from(Instant.now()),
        )
    }

    fun listarPorChamado(chamadoId: UUID): List<LogChamadoResposta> =
        jdbc.query(
            "SELECT id, chamado_id, tipo_evento, autor_id, dados, criado_em FROM chamado_log WHERE chamado_id = ? ORDER BY criado_em, id",
            ::mapear,
            chamadoId,
        )

    fun listar(
        chamadoId: UUID?,
        autorId: UUID?,
        tipoEvento: String?,
        de: Instant?,
        ate: Instant?,
        pageable: Pageable,
    ): Page<LogChamadoResposta> {
        val filtros: List<Pair<String, Any>> =
            listOfNotNull(
                chamadoId?.let { "chamado_id = ?" to it },
                autorId?.let { "autor_id = ?" to it },
                tipoEvento?.let { "tipo_evento = ?" to it },
                de?.let { "criado_em >= ?" to Timestamp.from(it) },
                ate?.let { "criado_em <= ?" to Timestamp.from(it) },
            )
        val clausulas = filtros.map { it.first }
        val valores = filtros.map { it.second }
        val filtro = clausulas.takeIf { it.isNotEmpty() }?.joinToString(" AND ")?.let { " WHERE $it" } ?: ""
        val rows =
            jdbc.query(
                "SELECT id, chamado_id, tipo_evento, autor_id, dados, criado_em FROM chamado_log$filtro ORDER BY criado_em DESC LIMIT ? OFFSET ?",
                ::mapear,
                *(valores + pageable.pageSize + pageable.offset).toTypedArray(),
            )
        val total =
            jdbc.queryForObject("SELECT count(*) FROM chamado_log$filtro", Long::class.javaObjectType, *valores.toTypedArray()) ?: 0L
        return PageImpl(rows, pageable, total)
    }

    private fun mapear(
        rs: java.sql.ResultSet,
        rowNumber: Int,
    ) = LogChamadoResposta(
        rs.getObject("id", UUID::class.java),
        rs.getObject("chamado_id", UUID::class.java),
        rs.getString("tipo_evento"),
        rs.getObject("autor_id", UUID::class.java),
        mapper.readTree(rs.getString("dados")),
        rs.getTimestamp("criado_em").toInstant(),
    )
}
