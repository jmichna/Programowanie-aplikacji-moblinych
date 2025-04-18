package pl.wsei.pam.lab03

class MemoryGameLogic(private val maxMatches: Int) {
    private val valueFunctions: MutableList<() -> Int> = mutableListOf()
    private var matches: Int = 0

    fun process(value: () -> Int): GameStates {
        if (valueFunctions.size < 1) {
            valueFunctions.add(value)
            return GameStates.Matching
        }
        valueFunctions.add(value)
        val result = (valueFunctions[0]() == valueFunctions[1]())
        if (result) matches++

        valueFunctions.clear()

        return when {
            result && matches == maxMatches -> GameStates.Finished
            result -> GameStates.Match
            else -> GameStates.NoMatch
        }
    }
}