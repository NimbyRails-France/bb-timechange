package fr.nimby.timechange

import nimby.*

/** Le mod change la date avec les interventions natives sur les trains.
 * Le SDK possède le calendrier UTC et l'application sur le thread natif.
 * Une commande est consommée AVANT l'appel : aucun rejeu après une erreur. */
class TimeChange {
    private data class Change(val date: GameDateTime, val world: String, val generation: Long)
    private var pending: Change? = null
    fun reset() { pending = null }
    fun handle(context: ToolContext, event: ToolWindowEvent) = with(context) {
        if (event.worldId != worldId || event.generation != generation) { reset(); return@with }
        try {
            when(event.action) {
                "open", "refresh", "cancel" -> { reset(); edit(event, clock().dateTime()) }
                "preview" -> {
                    reset()
                    val v = event.values
                    val date = GameDateTime(v.getValue("year"), v.getValue("month"), v.getValue("day"),
                        v.getValue("hour"), v.getValue("minute"), v.getValue("second"))
                    pending = Change(date, worldId, generation)
                    showWindow(event, tr("confirm", "date" to date.toString()),
                        listOf(ToolButton("apply", tr("apply")), ToolButton("cancel", tr("cancel"))))
                }
                "apply" -> {
                    val change = pending ?: return@with
                    reset()
                    check(change.world == worldId && change.generation == generation) { "La partie a changé" }
                    log("BB Timechange request UTC=${change.date} recalculate=true world=$worldId generation=$generation")
                    // Le joueur confirme à la fois la date et les interventions.
                    // Le SDK conserve son autre mode pour les outils qui en ont besoin.
                    val result = changeTime(change.date, recalculateTrains = true)
                    val appliedDate = result.clock.dateTime()
                    log("BB Timechange applied UTC=$appliedDate interventions=${result.interventions}")
                    edit(event, appliedDate, tr("success", "date" to appliedDate.toString(), "count" to result.interventions))
                }
            }
        } catch(error: Exception) {
            reset()
            log("BB Timechange failed; no automatic retry: ${error.message}", LogLevel.Error)
            showWindow(event, tr(if(event.action == "apply") "uncertain" else "invalid"), listOf(ToolButton("refresh", tr("refresh"))))
        }
    }
    private fun ToolContext.edit(event: ToolWindowEvent, date: GameDateTime, message: String = tr("intro", "date" to date.toString())) {
        showWindow(event, message,
            listOf(ToolButton("preview", tr("preview")), ToolButton("refresh", tr("refresh"))),
            listOf(ToolNumberInput("year", tr("year"), date.year, 1, 9999), ToolNumberInput("month", tr("month"), date.month, 1, 12),
                ToolNumberInput("day", tr("day"), date.day, 1, 31), ToolNumberInput("hour", tr("hour"), date.hour, 0, 23),
                ToolNumberInput("minute", tr("minute"), date.minute, 0, 59), ToolNumberInput("second", tr("second"), date.second, 0, 59)))
    }
}
