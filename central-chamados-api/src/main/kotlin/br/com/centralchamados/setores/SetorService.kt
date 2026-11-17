package br.com.centralchamados.setores

import br.com.centralchamados.setores.model.Setor
import br.com.centralchamados.setores.repository.SetorRepository
import br.com.centralchamados.shared.api.ErroApi
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SetorService(
    private val setores: SetorRepository,
) {
    fun listarAtivos(pageable: Pageable) = setores.findByAtivoTrue(pageable).map { it.toResposta() }

    fun listarAdmin(pageable: Pageable) = setores.findAll(pageable).map { it.toResposta() }

    fun criar(body: SetorRequest) = setores.save(Setor(nome = body.nome, descricao = body.descricao)).toResposta()

    @Transactional
    fun atualizar(
        id: UUID,
        body: SetorRequest,
    ): SetorResposta {
        val setor = setores.findById(id).orElseThrow { naoEncontrado() }
        setor.nome = body.nome
        setor.descricao = body.descricao
        return setores.save(setor).toResposta()
    }

    @Transactional
    fun desativar(id: UUID) {
        setores.findById(id).orElseThrow { naoEncontrado() }.ativo = false
    }

    private fun Setor.toResposta() = SetorResposta(id, nome, descricao, ativo)

    private fun naoEncontrado() = ErroApi(HttpStatus.NOT_FOUND, "SETOR_NAO_ENCONTRADO", "Setor não encontrado")
}
