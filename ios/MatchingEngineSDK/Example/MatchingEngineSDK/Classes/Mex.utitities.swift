//// misc extensions

import Foundation

import GoogleMaps

func getColorByHex(rgbHexValue: UInt32, alpha: Double = 1.0) -> UIColor
{
    let red = Double((rgbHexValue & 0xFF0000) >> 16) / 256.0
    let green = Double((rgbHexValue & 0xFF00) >> 8) / 256.0
    let blue = Double((rgbHexValue & 0xFF)) / 256.0

    return UIColor(red: CGFloat(red), green: CGFloat(green), blue: CGFloat(blue), alpha: CGFloat(alpha))
}

func getColorByHex(_ rgbHexValue: UInt32) -> UIColor
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

extension CGRect // JT 18.11.19
{
    public init(_ x: CGFloat, _ y: CGFloat, _ width: CGFloat, _ height: CGFloat)
    {
        self.init(x: x, y: y, width: width, height: height)
    }
}

extension UIImage
{
    func rotate(radians: Float) -> UIImage?
    {
        var newSize = CGRect(origin: CGPoint.zero, size: size).applying(CGAffineTransform(rotationAngle: CGFloat(radians))).size
        // Trim off the extremely small float value to prevent core graphics from rounding it up
        newSize.width = floor(newSize.width)
        newSize.height = floor(newSize.height)

        UIGraphicsBeginImageContextWithOptions(newSize, true, scale)
        let context = UIGraphicsGetCurrentContext()!

        // Move origin to middle
        context.translateBy(x: newSize.width / 2, y: newSize.height / 2)
        // Rotate around middle
        context.rotate(by: CGFloat(radians))
        // Draw the image at its center
        draw(in: CGRect(x: -size.width / 2, y: -size.height / 2, width: size.width, height: size.height))

        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return newImage
    }
}

extension String
{
    func doesStringStartWithCap() -> Bool
    {
        let firstChar = self[0] // s.characters[0]
        return firstChar >= "A" && firstChar <= "Z"
    }

    subscript(i: Int) -> Character
    {
        return self[index(startIndex, offsetBy: i)]
    }
}

extension UIImage
{
    /// SwifterSwift: UIImage Cropped to CGRect.
    ///
    /// - Parameter rect: CGRect to crop UIImage to.
    /// - Returns: cropped UIImage
    public func cropped(to rect: CGRect) -> UIImage
    {
        guard rect.size.width < size.width && rect.size.height < size.height else { return self }
        guard let image: CGImage = cgImage?.cropping(to: rect) else { return self }
        
        return UIImage(cgImage: image)
    }
}


class MovingAverage //   Rolling average
{
    var samples: Array<Double>
    var sampleCount = 0
    var period = 5
    
    init(period: Int = 5)
    {
        self.period = period
        
        samples = Array<Double>()
    }
    
    var average: Double
    {
        //   let sum: Double = samples.reduce(0, combine: +)
        let sum: Double = samples.reduce(0,   +)
        if period > samples.count
        {
            return sum / Double(samples.count)
        }
        else
        {
            return sum / Double(period)
        }
    }
    
    func addSample(value: Double) -> Double
    {
        let pos = Int(fmodf(Float(sampleCount), Float(period)))
        sampleCount += 1
        if pos >= samples.count
        {
            samples.append(value)
        }
        else
        {
            samples[pos] = value
        }
        return average
    }
}
