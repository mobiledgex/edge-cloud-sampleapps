//
//  Hellcat.swift
//  Pods
//
//  Created by seanmcneil on 3/9/17.
//
//

import UIKit
import AVFoundation

public enum HellcatError: Error {
    case FailureToObtainAssetTrack
    case FailureToCreateAssetReader(Error)
    case FailureToObtainImage
}

public final class Hellcat {    
    public init() { }
    
    public func imageFrames(for videoURL: URL, progress: @escaping (Progress) -> (), success: ([UIImage]) -> (), failure: (HellcatError) -> ()) {
        var images = [UIImage]()
        let asset = AVAsset(url: videoURL)
        guard let assetTrack = asset.tracks(withMediaType: AVMediaTypeVideo).first else {
            failure(.FailureToObtainAssetTrack)
            
            return
        }
        
        let fps = Double(assetTrack.nominalFrameRate)
        let duration = asset.duration
        
        var assetReader: AVAssetReader?
        do {
            assetReader = try AVAssetReader(asset: asset)
        } catch {
            failure(.FailureToCreateAssetReader(error))
        }
        
        let assetReaderOutputSettings = [
            kCVPixelBufferPixelFormatTypeKey as String: NSNumber(value: kCVPixelFormatType_32BGRA)
        ]
        let assetReaderOutput = AVAssetReaderTrackOutput(track: assetTrack, outputSettings: assetReaderOutputSettings)
        assetReaderOutput.alwaysCopiesSampleData = false
        assetReader?.add(assetReaderOutput)
        assetReader?.startReading()
        
        let currentProgress = Progress(totalUnitCount: Int64(fps * duration.seconds))
        var frameCount: Int64 = 0
        var sample = assetReaderOutput.copyNextSampleBuffer()
        
        while (sample != nil) {
            guard let image = sample?.uiImage else {
                failure(.FailureToObtainImage)
                
                break
            }
            
            images.append(image)
            frameCount += 1
            sample = assetReaderOutput.copyNextSampleBuffer()
            
            currentProgress.completedUnitCount = frameCount
            progress(currentProgress)
        }
        
        success(images)
    }
}
