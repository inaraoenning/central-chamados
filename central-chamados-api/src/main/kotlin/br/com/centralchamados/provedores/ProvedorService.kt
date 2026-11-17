package br.com.centralchamados.provedores

import br.com.centralchamados.provedores.model.Provedor
import br.com.centralchamados.provedores.repository.ProvedorRepository
import br.com.centralchamados.shared.api.ErroApi
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProvedorService(
    private val provedores: ProvedorRepository,
) {
    fun listar(pageable: Pageable) = provedores.findAll(pageable).map { it.toResposta() }

    fun criar(body: ProvedorRequest) = provedores.save(Provedor(nome = body.nome, cnpj = body.cnpj)).toResposta()

    @Transactional
    fun atualizar(
        id: UUID,
        body: ProvedorRequest,
    ): ProvedorResposta {
        val provedor = provedores.findById(id).orElseThrow { naoEncontrado() }
        provedor.nome = body.nome
        provedor.cnpj = body.cnpj
        return provedores.save(provedor).toResposta()
    }

    @Transactional
    fun desativar(id: UUID) {
        provedores.findById(id).orElseThrow { naoEncontrado() }.ativo = false
    }

    private fun Provedor.toResposta() = ProvedorResposta(id, nome, cnpj, ativo, criadoEm)

    private fun naoEncontrado() = ErroApi(HttpStatus.NOT_FOUND, "PROVEDOR_NAO_ENCONTRADO", "Provedor não encontrado")
}
