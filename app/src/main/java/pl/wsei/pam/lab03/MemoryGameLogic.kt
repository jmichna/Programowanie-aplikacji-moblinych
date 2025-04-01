class MemoryGameLogic(private val maxMatches: Int) {

    private var valueFunctions: MutableList<() -> Int> = mutableListOf()
    private var matches: Int = 0

    fun process(value: () -> Int): GameStates {
        if (valueFunctions.size < 1) {
            valueFunctions.add(value)
            return GameStates.Matching
        }
        valueFunctions.add(value)

        val result = valueFunctions[0]() == valueFunctions[1]()

        if (result) {
            matches++
        }
        valueFunctions.clear()

        return when (result) {
            true -> if (matches == maxMatches) GameStates.Finished else GameStates.Match
            false -> GameStates.NoMatch
        }
    }
}