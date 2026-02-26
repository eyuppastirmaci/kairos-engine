package dev.kairos.engine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KairosEngineApplication

fun main(args: Array<String>) {
	runApplication<KairosEngineApplication>(*args)
}
