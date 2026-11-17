package br.com.centralchamados.setores.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "setor")
class Setor(
    @Id var id: UUID = UUID.randomUUID(),
    var nome: String = "",
    var descricao: String? = null,
    var ativo: Boolean = true,
)
