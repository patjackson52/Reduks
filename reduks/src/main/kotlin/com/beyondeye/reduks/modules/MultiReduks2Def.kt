package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * Created by daely on 8/3/2016.
 */
class MultiReduksDef {
    companion object {
        //calculate context for multiple modules
        fun multiContext(vararg ctxs:ReduksContext):ReduksContext {
            val res=ctxs.reduce { prevCtx, ctx -> prevCtx+ctx }
            if(ctxs.toSet().size<ctxs.size)
                throw IllegalArgumentException("Invalid MultiContext: when combining multiple modules, each module MUST have a distinct context!: $res")
            return res
        }

        fun <S1 : Any, S2 : Any> create(storeFactory_: StoreFactory<MultiState2<S1, S2>>,
                                        m1: ReduksModule.Def<S1>,
                                        m2: ReduksModule.Def<S2>): ReduksModule.Def<MultiState2<S1, S2>> {
            val mctx=multiContext(m1.ctx, m2.ctx)
            return ReduksModule.Def<MultiState2<S1, S2>>(
                    ctx = mctx,
                    storeFactory = MultiStore2.Factory<S1, S2>(storeFactory_.ofType(), m1.ctx, m2.ctx),
                    initialState = MultiState2(mctx, m1.initialState, m2.initialState),
                    startAction = MultiActionWithContext(
                            ActionWithContext(m1.startAction, m1.ctx),
                            ActionWithContext(m2.startAction, m2.ctx)),
                    stateReducer = MultiReducer2<S1, S2>(m1, m2),
                    subscriberBuilder = StoreSubscriberBuilder { store ->
                        if (store !is MultiStore2<*, *>) throw IllegalArgumentException("error")
                        val selector = SelectorBuilder<MultiState2<S1, S2>>()
                        val s1sel = selector.withSingleField { s1 }
                        val s2sel = selector.withSingleField { s2 }
                        @Suppress("UNCHECKED_CAST")
                        val sub1 = m1.subscriberBuilder.build(store.store1 as Store<S1>)
                        @Suppress("UNCHECKED_CAST")
                        val sub2 = m2.subscriberBuilder.build(store.store2 as Store<S2>)
                        StoreSubscriber { newS ->
                            s1sel.onChangeIn(newS) {
                                sub1.onStateChange(newS.s1)
                            }
                            s2sel.onChangeIn(newS) {
                                sub2.onStateChange(newS.s2)
                            }
                        }
                    })
        }
    }

}


