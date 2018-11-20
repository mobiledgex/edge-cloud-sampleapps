//
//  TwoLineRow.swift
//  Example
//
//  Created by meta30 on 11/4/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Foundation
import Eureka   // JT 18.11.04

// Custom Cell with value type: Bool
// The cell is defined using a .xib, so we can set outlets :)
public class TwoLineCell: Cell<String>, CellType
{
   // @IBOutlet weak var switchControl: UISwitch!
 //   @IBOutlet weak public var textLabel2: UILabel?
    @IBOutlet var textLabel2: UILabel!
  //  @IBOutlet weak public var detailLabel2: UILabel? // JT 18.11.04
    @IBOutlet var detailLabel2: UILabel!
    
    public override func setup() {
        super.setup()
 
    }
    
    func switchValueChanged(){
        //row.value = switchControl.on
        row.updateCell() // Re-draws the cell which calls 'update' bellow
    }
    
    public override func update() {
        super.update()
     //   backgroundColor = (row.value ?? false) ? .white : .black
    }
}

// The custom Row also has the cell: CustomCell and its correspond value
public final class TwoLineRow: Row<TwoLineCell>, RowType {
    required public init(tag: String?) {
        super.init(tag: tag)
        // We set the cellProvider to load the .xib corresponding to our cell
        cellProvider = CellProvider<TwoLineCell>(nibName: "TwoLineCell")
    }
}

