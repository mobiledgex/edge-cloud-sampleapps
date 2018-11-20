//
//  ViewController.swift
//  Hellcat
//
//  Created by seanmcneil on 03/09/2017.
//  Copyright (c) 2017 seanmcneil. All rights reserved.
//

import UIKit
import Hellcat

class ViewController: UIViewController {
    @IBOutlet weak var imageView: UIImageView!

    override func viewDidLoad() {
        super.viewDidLoad()
        
        let path = Bundle.main.path(forResource: "Chaplin", ofType: "mp4")
        let url = URL(fileURLWithPath: path!)
        
        let hellcat = Hellcat()
        hellcat.imageFrames(for: url, progress: { (progress) in
            let percent = (progress.fractionCompleted * 100).roundTo(places: 2)
            print("\(percent)%")
        }, success: { (images) in
            let index = Int(arc4random_uniform(UInt32(images.count)) + 1)
            let image = images[index]
            imageView.image = image
        }) { (error) in
            print(error.localizedDescription)
        }
    }
}

extension Double {
    func roundTo(places:Int) -> Double {
        let divisor = pow(10.0, Double(places))
        return (self * divisor).rounded() / divisor
    }
}
