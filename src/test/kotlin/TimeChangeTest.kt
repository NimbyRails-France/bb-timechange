import kotlin.test.*
import nimby.*
import nimby.internal.ToolAccess
import fr.nimby.timechange.TimeChange

class TimeChangeTest {
    private val world = "fixture-world"
    private val values = mapOf("year" to 2026, "month" to 9, "day" to 28, "hour" to 12, "minute" to 34, "second" to 56)
    private fun event(action: String, generation: Long = 1, fields: Map<String, Int> = values) =
        ToolWindowEvent("clock", action, fields, 1, world, generation)
    private class Host {
        var writes = 0
        var clockReads = 0
        var recalculated = false
        var seconds = 0L
        var failWrite = false
        val forms = mutableListOf<String>()
        fun call(op: Int, integers: LongArray, numbers: DoubleArray, text: ByteArray): Int {
            assertTrue(numbers.isEmpty())
            when(op) {
                7 -> Unit // Diagnostic only.
                10 -> { clockReads++; integers[0] = seconds; integers[1] = 1234 }
                11 -> {
                    writes++; seconds = integers[0]; recalculated = integers[1] != 0L
                    if(failWrite) return 6 // An error can follow a real change.
                    integers[0] = seconds; integers[1] = 1234; integers[2] = if(recalculated) 2 else 0
                }
                12 -> forms.add(text.decodeToString())
                else -> error("Unexpected operation: $op")
            }
            return 0
        }
    }
    @Test fun utcCalendarKnownDatesAndLimits() {
        val cases = listOf(
            GameDateTime(1,1,1) to -62135596800L,
            GameDateTime(1969,12,31,23,59,59) to -1L,
            GameDateTime(1970,1,1) to 0L,
            GameDateTime(2000,2,29) to 951782400L,
            GameDateTime(9999,12,31,23,59,59) to 253402300799L)
        cases.forEach { (date, seconds) -> assertEquals(seconds, date.toUtcSeconds()); assertEquals(date, GameDateTime.fromUtcSeconds(seconds)) }
        assertFailsWith<IllegalArgumentException> { GameDateTime(1900,2,29) }
        assertFailsWith<IllegalArgumentException> { GameDateTime(2026,4,31) }
        assertFailsWith<IllegalArgumentException> { GameDateTime(2026,1,1,24) }
        assertFailsWith<IllegalArgumentException> { GameDateTime.fromUtcSeconds(253402300800L) }
    }
    @Test fun preparationAndCancelNeverChangeClock() {
        val host = Host(); val mod = TimeChange()
        ToolAccess.withContext(world, 1, host::call) { context ->
            listOf("open", "preview", "cancel", "apply").forEach { mod.handle(context, event(it)) }
        }
        assertEquals(0, host.writes)
    }
    @Test fun confirmationChangesClockWithInterventionsExactlyOnce() {
        val host = Host(); val mod = TimeChange()
        ToolAccess.withContext(world, 1, host::call) { context ->
            mod.handle(context, event("preview"))
            assertEquals(0, host.writes)
            mod.handle(context, event("apply")); mod.handle(context, event("apply"))
        }
        assertEquals(1, host.writes); assertTrue(host.recalculated)
        assertEquals(GameDateTime(2026,9,28,12,34,56).toUtcSeconds(), host.seconds)
    }
    @Test fun uncertainCommandIsConsumedAndNeverReplayed() {
        val host = Host().apply { failWrite = true }; val mod = TimeChange()
        ToolAccess.withContext(world, 1, host::call) { context ->
            mod.handle(context, event("preview")); mod.handle(context, event("apply"))
            mod.handle(context, event("apply")); mod.handle(context, event("refresh"))
        }
        assertEquals(1, host.writes)
        assertTrue(host.forms.any { "uncertain" in it })
    }
    @Test fun changedSessionAndImpossibleDateDiscardPendingCommand() {
        val host = Host(); val mod = TimeChange()
        ToolAccess.withContext(world, 1, host::call) { mod.handle(it, event("preview")) }
        ToolAccess.withContext(world, 2, host::call) { mod.handle(it, event("apply")) }
        ToolAccess.withContext(world, 1, host::call) {
            mod.handle(it, event("preview", fields = values + ("month" to 2) + ("day" to 30)))
            mod.handle(it, event("apply"))
        }
        assertEquals(0, host.writes)
    }
    @Test fun toolWindowDeclarationsRejectDuplicateShortcuts() {
        assertFailsWith<IllegalArgumentException> {
            toolMod("demo", "Demo") { window("a", "A", "F8") {}; window("b", "B", "F8") {} }
        }
    }
    @Test fun editingAndDuplicateApplyDoNotPollTheClockOrCaptureTheNetwork() {
        val host = Host(); val mod = TimeChange()
        ToolAccess.withContext(world, 1, host::call) { context ->
            repeat(1000) { mod.handle(context, event("unhandled")) }
            assertEquals(0, host.clockReads)
            mod.handle(context, event("open")); assertEquals(1, host.clockReads)
            mod.handle(context, event("preview")); mod.handle(context, event("apply"))
            repeat(1000) { mod.handle(context, event("apply")) }
            assertEquals(1, host.clockReads); assertEquals(1, host.writes)
        }
    }
}
