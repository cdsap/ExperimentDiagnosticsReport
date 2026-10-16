package io.github.cdsap.experimentdiagnostics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.github.cdsap.experimentdiagnostics.cli.ReportCommand

class Main : CliktCommand(
    name = "experiment-diagnostics-report",
) {
    override fun run() = Unit
}

fun main(args: Array<String>) = Main().subcommands(ReportCommand()).main(args)
