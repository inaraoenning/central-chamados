package br.com.centralchamados.categorias.model

import br.com.centralchamados.setores.model.Setor
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "categoria")
class Categoria(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "setor_id") var setor: Setor = Setor(),
    var nome: String = "",
    var ativo: Boolean = true,
)
