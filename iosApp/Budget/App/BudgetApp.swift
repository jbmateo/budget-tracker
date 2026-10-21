import SwiftUI
import Shared

@main
struct BudgetApp: App {
    init() {
        #if DEBUG
        // UI tests pass `-uiTestResetDB` to start from an empty database and `-uiTestToday <yyyy-MM-dd>` to pin "today".
        // `-seedSampleData` fills an empty database with sample spending.
        let arguments = ProcessInfo.processInfo.arguments
        let reset = arguments.contains("-uiTestResetDB")
        let seed = arguments.contains("-seedSampleData")
        let fixedToday = arguments.firstIndex(of: "-uiTestToday").flatMap { i in
            arguments.indices.contains(i + 1) ? arguments[i + 1] : nil
        }
        #else
        let reset = false
        let fixedToday: String? = nil
        let seed = false
        #endif
        KoinHelper.shared.start(resetDatabase: reset, fixedToday: fixedToday, seedSample: seed)
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
    }
}
