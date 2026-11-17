package br.com.centralchamados

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CentralChamadosApplication

fun main(args: Array<String>) {
    runApplication<CentralChamadosApplication>(*args)
}
