package nimby.mod

import nimby.*
import fr.nimby.timechange.TimeChange

/** Outil autonome : aucune texture, aucun modèle de signal, aucune règle BAL. */
fun createMod(): ToolMod {
    val workflow = TimeChange()
    return toolMod(modInfo) {
        metadata(author = "NimbyRails France", name = tr("mod.name"), description = tr("mod.description"))
        window("clock", tr("title"), shortcut = "Ctrl+Shift+T") { event -> workflow.handle(this, event) }
        onStop { workflow.reset() }
    }
}
