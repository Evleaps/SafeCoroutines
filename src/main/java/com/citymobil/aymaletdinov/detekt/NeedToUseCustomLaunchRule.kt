package com.citymobil.aymaletdinov.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Created by Aymaletdinov Roman
 *
 * This rule forbids the use of standard Scope.launch(vararg).
 * To use it, you must add the Detekt static analyzer to your project
 *
 * @see https://github.com/detekt/detekt
 */
internal class NeedToUseCustomLaunchRule(config: Config) : Rule(config) {

    companion object {

        private const val TRIGGER_VALUE = ".launch {"
    }

    override val issue = Issue(
        id = "NeedToUseCustomLaunch",
        description = "Must use a custom launch ext from SafeCoroutinesExt instead \".launch { smth }\"",
        severity = Severity.CodeSmell,
        debt = Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        var offset = 0
        val lines = function.text.lines()

        for (line in lines) {
            offset += line.length
            if (line.contains(TRIGGER_VALUE)) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(function, offset),
                        message = "The function ${function.name} using Coroutines. " +
                                "You must use a custom launch ext from SafeCoroutinesExt instead \".launch { smth }\" here."
                    )
                )
            }

            offset += 1 // '\n'
        }
    }
}
