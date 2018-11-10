package com.beyondeye.reduks

import kotlin.test.*


/**
 * Created by Dario on 3/18/2016.
 * code ported from https://github.com/reactjs/reselect/blob/master/test/test_selector.js
 * and expanded
 * TODO: support
 */


class ReselectTest {
    data class StateA(val a: Int)

    @Test
    fun basicSelectorTest() {
        val selector = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(0)
        assertEquals(selector(state), 0)
        assertEquals(selector(state), 0)
        assertEquals(selector.recomputations, 1)
        assertEquals(selector(state.copy(a = 1)), 1)
        assertEquals(selector.recomputations, 2)
    }
    @Test
    fun signalChangedTest() {
        val selector = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(0)
        assertEquals(selector(state), 0)
        assertEquals(selector(state), 0)
        assertEquals(selector.recomputations, 1)
        selector.signalChanged()
        assertEquals(selector.recomputations, 2)
    }
    data class StateAB(val a: Int, val b: Int)
    data class StateABFloat(val a: Float, val b: Float)

    @Test
    fun basicSelectorWithMultipleKeysTest() {
        val selector = SelectorBuilder<StateAB>()
                .withField { a }
                .withField { b }
                .compute { a: Int, b: Int -> a + b }
        val state1 = StateAB(a = 1, b = 2)
        assertEquals(selector(state1), 3)
        assertEquals(selector(state1), 3)
        assertEquals(selector.recomputations, 1)
        val state2 = StateAB(a = 3, b = 2)
        assertEquals(selector(state2), 5)
        assertEquals(selector(state2), 5)
        assertEquals(selector.recomputations, 2)
    }
    data class StateABCFloat(val a:Float,val b:Float,val c:Float)
    @Test
    fun basicSelectorWithMultipleKeysByValueTest() {
        val selector = SelectorBuilder<StateABCFloat>()
                .withField { a }
                .withField { b }
                .compute { aa: Float, bb: Float -> aa + bb }
        val selectorByValue = SelectorBuilder<StateABCFloat>()
                .withFieldByValue { a }
                .withFieldByValue { b }
                .compute { aa: Float, bb: Float -> aa + bb }
        val state1 = StateABCFloat(a = 2.0f, b = 3.0f,c=0.0f)
        assertEquals(selector(state1), 5f)
        assertEquals(selectorByValue(state1), 5f)
        assertEquals(selector.recomputations, 1)
        assertEquals(selectorByValue.recomputations, 1)
        //a state with equal a,b fields by value
        val state2 = state1.copy(c=-1.0f)
        assertEquals(selectorByValue(state2), 5f)
        assertEquals(selector(state2), 5f)
        //regular selector (with argument by compared by reference) is recomputed, because the state is a different object
        assertEquals(selector.recomputations, 2)
        //selector with arguments compared by value IS NOT RECOMPUTED, because input arguments, when compared by value are the same
        assertEquals(selectorByValue.recomputations, 1)
    }

    data class StateSubStateA(val sub: StateA)

    @Test
    fun memoizedCompositeArgumentsTest() {
        val selector = SelectorBuilder<StateSubStateA>()
                .withField { sub }
                .compute { sub: StateA -> sub }
        val state1 = StateSubStateA(StateA(1))
        assertEquals(selector(state1), StateA(1))
        assertEquals(selector(state1), StateA(1))
        assertEquals(selector.recomputations, 1)
        val state2 = StateSubStateA(StateA(2))
        assertEquals(selector(state2), StateA(2))
        assertEquals(selector.recomputations, 2)
    }


    @Test
    fun chainedSelectorTest() {
        val selector1 = SelectorBuilder<StateSubStateA>()
                .withField { sub }
                .compute { sub: StateA -> sub }
        val selector2 = SelectorBuilder<StateSubStateA>()
                .withSelector(selector1)
                .compute { sub: StateA -> sub.a }
        val state1 = StateSubStateA(StateA(1))
        assertEquals(selector2(state1), 1)
        assertEquals(selector2(state1), 1)
        assertEquals(selector2.recomputations, 1)
        val state2 = StateSubStateA(StateA(2))
        assertEquals(selector2(state2), 2)
        assertEquals(selector2.recomputations, 2)
    }


    @Test
    fun recomputationsCountTest() {
        val selector = SelectorBuilder<StateA>()
                .withField { a }
                .compute { a: Int -> a }

        val state1 = StateA(a = 1)
        assertEquals(selector(state1), 1)
        assertEquals(selector(state1), 1)
        assertEquals(selector.recomputations, 1)
        val state2 = StateA(a = 2)
        assertEquals(selector(state2), 2)
        assertEquals(selector.recomputations, 2)

        assertEquals(selector(state1), 1)
        assertEquals(selector(state1), 1)
        assertEquals(selector.recomputations, 3)
        assertEquals(selector(state2), 2)
        assertEquals(selector.recomputations, 4)
    }
    @Test
    fun primitiveFieldTest() {
        val selA = SelectorBuilder<StateABFloat>()
                .withSingleField {  a }
        val selAByValue = SelectorBuilder<StateABFloat>()
                .withSingleFieldByValue {  a }

        val state1 = StateABFloat(a = 1.0f,b=11.0f)

        val a1=selA(state1)
        assertEquals(a1, 1.0f)
        assertEquals(selA.recomputations, 1)
        //-----
        val a1_v=selAByValue(state1)
        assertEquals(a1_v, 1.0f)
        assertEquals(selAByValue.recomputations, 1)

        val state2= state1.copy(b=22.0f)
        val a2=selA(state2)
        //although we did not change a, (we changed b),
        //we recomputed because default memoization is by reference, not by value
        assertEquals(a2, 1.0f)
        assertEquals(selA.recomputations, 2)
        //----------------------------
        //now use memoization by value
        val a2_b=selAByValue(state2)
        assertEquals(a2_b, 1.0f)
        //NO recomputations!!!
        assertEquals(selAByValue.recomputations, 1)
    }
    @Test
    fun isChangedTest() {
        val selector = SelectorBuilder<StateA>()
                .withField { a }
                .compute { a: Int -> a }
        val state1 = StateA(a = 1)
        assertEquals(selector(state1), 1)
        assertTrue(selector.isChanged())
        selector.resetChanged()
        assertEquals(selector(state1), 1)
        assertFalse(selector.isChanged())
        val state2 = StateA(a = 2)
        assertEquals(selector(state2), 2)
        assertTrue(selector.isChanged())
    }

    data class State3(val p1: Double, val p2: Double, val p3: Double)

    @Test
    fun args3Test() {
        val selector = SelectorBuilder<State3>()
                .withField { p1 }
                .withField { p2 }
                .withField { p3 }
                .compute { p1: Double, p2: Double, p3: Double -> p1 / p2 / p3 }
        val state = State3(1.0, 2.0, 3.0)
        assertEquals(selector(state), 1.0 / 2.0 / 3.0)
    }

    data class State4(val p1: Double, val p2: Double, val p3: Double, val p4: Double)

    @Test
    fun args4Test() {
        val selector = SelectorBuilder<State4>()
                .withField { p1 }
                .withField { p2 }
                .withField { p3 }
                .withField { p4 }
                .compute { p1: Double, p2: Double, p3: Double, p4: Double -> p1 / p2 / p3 / p4 }
        val state = State4(1.0, 2.0, 3.0, 4.0)
        assertEquals(selector(state), 1.0 / 2.0 / 3.0 / 4.0)
    }

    data class State5(val p1: Double, val p2: Double, val p3: Double, val p4: Double, val p5: Double)

    @Test
    fun args5Test() {
        val selector = SelectorBuilder<State5>()
                .withField { p1 }
                .withField { p2 }
                .withField { p3 }
                .withField { p4 }
                .withField { p5 }
                .compute { p1: Double, p2: Double, p3: Double, p4: Double, p5: Double -> p1 / p2 / p3 / p4 / p5 }

        val state = State5(1.0, 2.0, 3.0, 4.0, 5.0)
        assertEquals(selector(state), 1.0 / 2.0 / 3.0 / 4.0 / 5.0)
    }

    @Test
    fun singleFieldSelectorTest() {
        val sel4state = SelectorBuilder<State3>()
        val selp1 = sel4state.withSingleField { p1 }
        val selp2 = sel4state.withSingleField { p2 }
        val selp3 = sel4state.withSingleField { p3 }

        val state = State3(1.0, 2.0, 3.0)
        assertEquals(selp1(state), 1.0)
        assertEquals(selp2(state), 2.0)
        assertEquals(selp3(state), 3.0)
    }

    /*
    //test for short syntax for single field selector disabled because of kotlin compiler bug
    @Test
    fun singleFieldSelectorShortSyntaxText() {
        val sel4state = SelectorFor<State3>()
        val selp1 = sel4state{ p1 }
        val selp2 = sel4state{ p2 }
        val selp3 = sel4state{ p3 }

        val state = State3(1.0, 2.0, 3.0)
        assertThat(selp1(state)).isEqualTo(1.0)
        assertThat(selp2(state)).isEqualTo(2.0)
        assertThat(selp3(state)).isEqualTo(3.0)
    }
    */
    @Test
    fun onChangeTest() {
        val sel_a = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(a = 0)
        assertEquals(sel_a(state), 0)
        val changedState = state.copy(a = 1)
        var firstChangedA: Int? = null
        sel_a.onChangeIn(changedState) {
            firstChangedA = it
        }
        var secondChangedA: Int? = null
        sel_a.onChangeIn(changedState) {
            secondChangedA = it
        }
        assertEquals(firstChangedA, 1)
        assertNull(secondChangedA)

    }
    @Test
    fun onChangeConditionalTest() {
        val sel_a = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(a = 0)
        assertEquals(sel_a(state), 0)
        val changedState = state.copy(a = 1)
        var firstChangedA: Int? = null
        //this first time the selector is not run, because condition is false
        sel_a.onChangeIn(changedState,false) {
            firstChangedA = it
        }
        var secondChangedA: Int? = null
        //this second time the selector is run, because condition is true
        sel_a.onChangeIn(changedState,true) {
            secondChangedA = it
        }
        assertNull(firstChangedA)
        assertEquals(secondChangedA, 1)
    }

    @Test
    fun whenChangedTest() {
        val sel_a = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(a = 0)
        assertEquals(sel_a(state), 0)
        val changedState = state.copy(a = 1)
        var firstChangedA: Int? = null
        var secondChangedA: Int? = null
        with(changedState) {
            whenChangeOf(sel_a) {
                firstChangedA = it
            }
            whenChangeOf(sel_a) {
                secondChangedA = it
            }
            assertEquals(firstChangedA, 1)
            assertNull(secondChangedA)

        }
    }
}

