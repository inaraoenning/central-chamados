package br.com.centralchamados.provedores.repository

import br.com.centralchamados.provedores.model.Provedor
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProvedorRepository : JpaRepository<Provedor, UUID>
