package sample

import com.beyondeye.reduks.Action
import com.beyondeye.reduks.SimpleStore

/**
 * Simple example of a Multiplatform (Android & iOS) app using ReKotlin.
 * In a more production app these would be put into appropriate files/packages.
 */

data class GameState(val ct: Int) {
    companion object {
        val INITIAL_STATE = GameState(1)
    }
}

class ButtonClickAction : Action

fun reducer(state: GameState, action: Any): GameState {
    return when (action) {
        is ButtonClickAction -> state!!.copy(ct = state.ct + 1)
        else -> throw IllegalArgumentException("Unhandled Action")
    }
}


val gameStore = SimpleStore(
        reducer = ::reducer,
        initialState = GameState.INITIAL_STATE)
