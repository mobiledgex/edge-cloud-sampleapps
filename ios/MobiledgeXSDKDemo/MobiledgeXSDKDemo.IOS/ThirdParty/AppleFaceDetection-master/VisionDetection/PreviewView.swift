//
//  PreviewView.swift
//  VisionDetection
//
//  Created by Wei Chieh Tseng on 09/06/2017.
//  Copyright © 2017 Willjay. All rights reserved.
//

import AVFoundation
import UIKit
import Vision

//var lastFaceRect = CGRect(0,0,0,0)  // JT 18.11.27

class PreviewView: UIView
{
    private var maskLayerMex = [CAShapeLayer]()    // JT 18.12.14
    private var maskLayer = [CAShapeLayer]()

    // MARK: AV capture properties

    var videoPreviewLayer: AVCaptureVideoPreviewLayer
    {
        return layer as! AVCaptureVideoPreviewLayer
    }

    var session: AVCaptureSession?
    {
        get
        {
            return videoPreviewLayer.session
        }

        set
        {
            videoPreviewLayer.session = newValue
        }
    }

    override class var layerClass: AnyClass
    {
        return AVCaptureVideoPreviewLayer.self
    }

    // Create a new layer drawing the bounding box
    private func createLayer(in rect: CGRect) -> CAShapeLayer
    {
        let mask = CAShapeLayer()
        mask.frame = rect
        mask.cornerRadius = 10
        mask.opacity = 0.75
        mask.borderColor = UIColor.yellow.cgColor   // JT 18.12.14 built in FD
        mask.borderWidth = 2.0

        maskLayer.append(mask)
        layer.insertSublayer(mask, at: 1)

        return mask
    }
    
    private func createLayer2(in rect: CGRect) -> CAShapeLayer
    {
        let mask = CAShapeLayer()
        mask.frame = rect
        mask.cornerRadius = 10
        mask.opacity = 0.75
        mask.borderColor = UIColor.blue.cgColor // JT 18.11.27
        mask.borderWidth = 2.0
        
        maskLayer.append(mask)   // JT 18.12.14
        layer.insertSublayer(mask, at: 1)
        
        return mask
    }
    
    private func createLayer3(in rect: CGRect) -> CAShapeLayer  // JT 18.12.13
    {
        let mask = CAShapeLayer()
        mask.frame = rect
        mask.cornerRadius = 10
        mask.opacity = 0.75
        mask.borderColor = UIColor.red.cgColor  // JT 18.12.13
        mask.borderWidth = 2.0
        
        maskLayerMex.append(mask)
        layer.insertSublayer(mask, at: 1)
        
        return mask
    }
    
    private func createLayer4(in rect: CGRect) -> CAShapeLayer
    {
        let mask = CAShapeLayer()
        mask.frame = rect
        mask.cornerRadius = 10
        mask.opacity = 0.75
        mask.borderColor = UIColor.green.cgColor    // JT 18.12.13
        mask.borderWidth = 3.0  // JT 18.12.14
        
        maskLayerMex.append(mask)
        layer.insertSublayer(mask, at: 1)
        
        return mask
    }
    
    func getFaceBounds(face: VNFaceObservation) -> CGRect
    {
        let transform = CGAffineTransform(scaleX: 1, y: -1).translatedBy(x: 0, y: -frame.height)
        
        let translate = CGAffineTransform.identity.scaledBy(x: frame.width, y: frame.height)
        
        // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
        let facebounds = face.boundingBox.applying(translate).applying(transform)

        return facebounds
    }
    
    func getFaceBounds( rect:CGRect, hint sentImageSize: CGSize) -> CGRect
    {
        let transform = CGAffineTransform(scaleX: 1, y: 1).translatedBy(x: 0, y: 0)
        
        let ratioW = frame.width   / (sentImageSize.width * 2)  // JT 18.11.28
        let ratioH = frame.height   / (sentImageSize.height * 2)  // JT 18.11.28
        
        let translate = CGAffineTransform.identity.scaledBy(x: ratioW, y: ratioH)
        
        // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
        let facebounds = rect.applying(translate).applying(transform)

        return facebounds
    }
    
    func drawFaceboundingBox(face: VNFaceObservation)
    {
        let facebounds = getFaceBounds(face:face)
        
        _ = createLayer(in: facebounds) // yellow
     }
    
    func drawFaceboundingBox2( rect:CGRect, hint sentImageSize: CGSize)    // JT 18.11.27
    {

        // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
        let facebounds = getFaceBounds( rect: rect, hint: sentImageSize)

        _ = createLayer2(in: facebounds)    // JT 18.11.27 blue
    }
    
    
    func drawFaceboundingBoxCloud( rect:CGRect, hint sentImageSize: CGSize)   -> CGRect  // JT 18.11.27
    {
        // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
        let facebounds = getFaceBounds( rect: rect, hint: sentImageSize)
        
        _ = createLayer3(in: facebounds)    // green
        
        return facebounds   // JT 18.12.14
   }
    
    func drawFaceboundingBoxEdge( rect:CGRect, hint sentImageSize: CGSize)   -> CGRect   // JT 18.12.13
    {
        // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
        let facebounds = getFaceBounds( rect: rect, hint: sentImageSize)

        _ = createLayer4(in: facebounds)    // JT 18.12.14 red
        
        return facebounds   // JT 18.12.14
    }

    // MARK: -
    
    func drawFaceWithLandmarks(face: VNFaceObservation)
    {
        let transform = CGAffineTransform(scaleX: 1, y: -1).translatedBy(x: 0, y: -frame.height)

        let translate = CGAffineTransform.identity.scaledBy(x: frame.width, y: frame.height)

        // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
        let facebounds = face.boundingBox.applying(translate).applying(transform)

        // Draw the bounding rect
        let faceLayer = createLayer(in: facebounds)

        // Draw the landmarks
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.nose)!, isClosed: false)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.noseCrest)!, isClosed: false)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.medianLine)!, isClosed: false)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.leftEye)!)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.leftPupil)!)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.leftEyebrow)!, isClosed: false)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.rightEye)!)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.rightPupil)!)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.rightEye)!)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.rightEyebrow)!, isClosed: false)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.innerLips)!)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.outerLips)!)
        drawLandmarks(on: faceLayer, faceLandmarkRegion: (face.landmarks?.faceContour)!, isClosed: false)
    }

    func drawLandmarks(on targetLayer: CALayer, faceLandmarkRegion: VNFaceLandmarkRegion2D, isClosed: Bool = true)
    {
        let rect: CGRect = targetLayer.frame
        var points: [CGPoint] = []

        for i in 0 ..< faceLandmarkRegion.pointCount
        {
            let point = faceLandmarkRegion.normalizedPoints[i]
            points.append(point)
        }

        let landmarkLayer = drawPointsOnLayer(rect: rect, landmarkPoints: points, isClosed: isClosed)

        // Change scale, coordinate systems, and mirroring
        landmarkLayer.transform = CATransform3DMakeAffineTransform(
            CGAffineTransform.identity
                .scaledBy(x: rect.width, y: -rect.height)
                .translatedBy(x: 0, y: -1)
        )

        targetLayer.insertSublayer(landmarkLayer, at: 1)
    }

    func drawPointsOnLayer(rect _: CGRect, landmarkPoints: [CGPoint], isClosed: Bool = true) -> CALayer
    {
        let linePath = UIBezierPath()
        linePath.move(to: landmarkPoints.first!)

        for point in landmarkPoints.dropFirst()
        {
            linePath.addLine(to: point)
        }

        if isClosed
        {
            linePath.addLine(to: landmarkPoints.first!)
        }

        let lineLayer = CAShapeLayer()
        lineLayer.path = linePath.cgPath
        lineLayer.fillColor = nil
        lineLayer.opacity = 1.0
        lineLayer.strokeColor = UIColor.green.cgColor
        lineLayer.lineWidth = 0.02

        return lineLayer
    }

    func removeMask()
    {
        for mask in maskLayer
        {
            mask.removeFromSuperlayer()
        }
        maskLayer.removeAll()
    }
    
    func removeMaskLayerMex()   // JT 18.12.14
    {
        for mask in maskLayerMex
        {
            mask.removeFromSuperlayer()
        }
        maskLayerMex.removeAll()
    }
}
