import SwiftUI
import Shared

@main
struct BudgetApp: App {
    init() {
        #if DEBUG
        // UI tests pass `-uiTestResetDB` to start from an empty database and `-uiTestToday <yyyy-MM-dd>` to pin "today".
        let arguments = ProcessInfo.processInfo.arguments
        let reset = arguments.contains("-uiTestResetDB")
        let fixedToday = arguments.firstIndex(of: "-uiTestToday").flatMap { i in
            arguments.indices.contains(i + 1) ? arguments[i + 1] : nil
        }
        #else
        let reset = false
        let fixedToday: String? = nil
        #endif
        KoinHelper.shared.start(resetDatabase: reset, fixedToday: fixedToday)
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
    }
}
