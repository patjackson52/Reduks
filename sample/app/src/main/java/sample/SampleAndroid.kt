package sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.beyondeye.reduks.StoreSubscriber
import com.beyondeye.reduks.StoreSubscription

class MainActivity : AppCompatActivity(), StoreSubscriber<GameState> {
    override fun onStateChange() {
        ctTxtView.text = "ct = ${gameStore.state.ct}"
    }

    lateinit var ctTxtView: TextView
    lateinit var button: Button
    var storeSubscription: StoreSubscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ctTxtView = findViewById(R.id.txt_ct)
        button = findViewById(R.id.button)
        button.setOnClickListener {
            gameStore.dispatch(ButtonClickAction())
        }
        storeSubscription = gameStore.subscribe(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        storeSubscription?.unsubscribe()
    }
}