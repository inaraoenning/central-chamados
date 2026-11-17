package br.com.centralchamados.categorias

import br.com.centralchamados.categorias.model.Categoria
import br.com.centralchamados.categorias.repository.CategoriaRepository
import br.com.centralchamados.setores.repository.SetorRepository
import br.com.centralchamados.shared.api.ErroApi
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoriaService(
    private val categorias: CategoriaRepository,
    private val setores: SetorRepository,
) {
    fun listarPorSetor(
        setorId: UUID,
        pageable: Pageable,
    ) = categorias.findBySetorIdAndAtivoTrue(setorId, pageable).map { it.toResposta() }

    fun listarAdmin(pageable: Pageable) = categorias.findAll(pageable).map { it.toResposta() }

    fun criar(body: CategoriaRequest): CategoriaResposta {
        val setor = setores.findById(body.setorId!!).orElseThrow { naoEncontrado() }
        if (!setor.ativo) throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "SETOR_INATIVO", "Setor inativo")
        return categorias.save(Categoria(setor = setor, nome = body.nome)).toResposta()
    }

    @Transactional
    fun atualizar(
        id: UUID,
        body: CategoriaRequest,
    ): CategoriaResposta {
        val categoria = categorias.findById(id).orElseThrow { naoEncontrado() }
        categoria.setor = setores.findById(body.setorId!!).orElseThrow { naoEncontrado() }
        categoria.nome = body.nome
        return categorias.save(categoria).toResposta()
    }

    @Transactional
    fun desativar(id: UUID) {
        categorias.findById(id).orElseThrow { naoEncontrado() }.ativo = false
    }

    private fun Categoria.toResposta() = CategoriaResposta(id, setor.id, nome, ativo)

    private fun naoEncontrado() = ErroApi(HttpStatus.NOT_FOUND, "CATEGORIA_NAO_ENCONTRADA", "Categoria não encontrada")
}
