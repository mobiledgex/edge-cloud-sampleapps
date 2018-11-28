//// misc extensions

//import CLLocation
import Foundation
import GoogleMaps   // JT 18.11.18

func getColorByHex(rgbHexValue: UInt32, alpha: Double = 1.0) -> UIColor // JT 18.11.05
{
    let red = Double((rgbHexValue & 0xFF0000) >> 16) / 256.0
    let green = Double((rgbHexValue & 0xFF00) >> 8) / 256.0
    let blue = Double((rgbHexValue & 0xFF)) / 256.0
    
    return UIColor(red: CGFloat(red), green: CGFloat(green), blue: CGFloat(blue), alpha: CGFloat(alpha))
}

func getColorByHex(_ rgbHexValue: UInt32) -> UIColor // JT 18.11.05
{
    let alpha = Double((rgbHexValue & 0xFF00_0000) >> 24) / 256.0
    let red = Double((rgbHexValue & 0xFF0000) >> 16) / 256.0
    let green = Double((rgbHexValue & 0xFF00) >> 8) / 256.0
    let blue = Double((rgbHexValue & 0xFF)) / 256.0
    
    return UIColor(red: CGFloat(red), green: CGFloat(green), blue: CGFloat(blue), alpha: CGFloat(alpha))
}

extension UIImage
{
    func imageWithColor(_ color1: UIColor) -> UIImage
    {
        UIGraphicsBeginImageContextWithOptions(size, false, scale)
        color1.setFill()
        
        let context = UIGraphicsGetCurrentContext()
        context?.translateBy(x: 0, y: size.height)
        context?.scaleBy(x: 1.0, y: -1.0)
        context?.setBlendMode(CGBlendMode.normal)
        
        let rect = CGRect(origin: .zero, size: CGSize(width: size.width, height: size.height))
        context?.clip(to: rect, mask: cgImage!)
        context?.fill(rect)
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return newImage!
    }
}

// JT 18.11.05 to do move to util

func textToImage(drawText text: String, inImage image: UIImage, atPoint point: CGPoint) -> UIImage
{
    let textColor = UIColor.white
    let textFont = UIFont(name: "Helvetica Bold", size: 18)!
    
    let scale = UIScreen.main.scale
    UIGraphicsBeginImageContextWithOptions(image.size, false, scale)
    
    let textFontAttributes = [
        NSAttributedString.Key.font: textFont,
        NSAttributedString.Key.foregroundColor: textColor,
        ] as [NSAttributedString.Key: Any]
    image.draw(in: CGRect(origin: CGPoint.zero, size: image.size))
    
    let rect = CGRect(origin: point, size: image.size)
    text.draw(in: rect, withAttributes: textFontAttributes)
    
    let newImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    
    return newImage!
}

extension CLLocationCoordinate2D: Equatable {}

public func == (lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool
{
    return lhs.latitude == rhs.latitude && lhs.longitude == rhs.longitude
}

extension UIImage
{
    func imageResize(sizeChange: CGSize) -> UIImage
    {
        let hasAlpha = true
        let scale: CGFloat = 0.0 // Use scale factor of main screen
        
        UIGraphicsBeginImageContextWithOptions(sizeChange, !hasAlpha, scale)
        draw(in: CGRect(origin: CGPoint.zero, size: sizeChange))
        
        let scaledImage = UIGraphicsGetImageFromCurrentImageContext()
        return scaledImage!
    }
}

extension CGPoint
{
    init(_ x: CGFloat, _ y: CGFloat)
    {
        self.init()
        self.x = x
        self.y = y
    }
}

extension CGRect    // JT 18.11.19
{
    public init(_ x: CGFloat, _ y: CGFloat, _ width: CGFloat, _ height: CGFloat)
    {
        self.init(x: x, y: y, width: width, height: height)
    }
}

extension UIImage {
    func rotate(radians: Float) -> UIImage? {
        var newSize = CGRect(origin: CGPoint.zero, size: self.size).applying(CGAffineTransform(rotationAngle: CGFloat(radians))).size
        // Trim off the extremely small float value to prevent core graphics from rounding it up
        newSize.width = floor(newSize.width)
        newSize.height = floor(newSize.height)
        
        UIGraphicsBeginImageContextWithOptions(newSize, true, self.scale)
        let context = UIGraphicsGetCurrentContext()!
        
        // Move origin to middle
        context.translateBy(x: newSize.width/2, y: newSize.height/2)
        // Rotate around middle
        context.rotate(by: CGFloat(radians))
        // Draw the image at its center
        self.draw(in: CGRect(x: -self.size.width/2, y: -self.size.height/2, width: self.size.width, height: self.size.height))
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return newImage
    }
}