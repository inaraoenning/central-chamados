package br.com.centralchamados.provedores.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provedor")
class Provedor(
    @Id var id: UUID = UUID.randomUUID(),
    var nome: String = "",
    var cnpj: String? = null,
    var ativo: Boolean = true,
    @Column(name = "criado_em") var criadoEm: Instant = Instant.now(),
)
