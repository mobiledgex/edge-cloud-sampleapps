import XCTest
@testable import SwiftGRPC

class SwiftGRPCTests: XCTestCase {
    func testExample() {
        // This is an example of a functional test case.
        // Use XCTAssert and related functions to verify your tests produce the correct results.
        XCTAssertEqual(SwiftGRPC().text, "Hello, World!")
    }


    static var allTests : [(String, (SwiftGRPCTests) -> () throws -> Void)] {
        return [
            ("testExample", testExample),
        ]
    }
}
