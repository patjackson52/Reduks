import UIKit
import app

class ViewController: UIViewController, ReKotlinStoreSubscriber {
    func doNewState(state: Any?) {
        
        if (state is GameState) {
            let castedState = state as! GameState
            label.text = "Ct = \(castedState.ct)"
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        SampleKt.gameStore.subscribe(subscriber: self)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    @IBAction func buttonClick(_ sender: Any) {
         SampleKt.gameStore.dispatch(action: ButtonClickAction())
    }
   
    @IBOutlet weak var label: UILabel!
    
}
