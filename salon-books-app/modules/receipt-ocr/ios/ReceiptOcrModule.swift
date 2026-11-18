import ExpoModulesCore
import UIKit
import Vision

/// Reads text from a receipt photo with Apple's Vision framework, entirely on the phone.
/// Returns every recognized line with its box (0...1, top-left origin) so JS can
/// rebuild rows like "FOILS 500CT      15.00" that Vision splits into two pieces.
public class ReceiptOcrModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ReceiptOcr")

    AsyncFunction("recognize") { (url: URL) throws -> [[String: Any]] in
      guard let data = try? Data(contentsOf: url),
            let image = UIImage(data: data),
            let cgImage = image.cgImage else {
        throw ImageReadException()
      }

      let request = VNRecognizeTextRequest()
      request.recognitionLevel = .accurate
      // Receipts are full of abbreviations and codes; autocorrect makes them worse.
      request.usesLanguageCorrection = false

      let handler = VNImageRequestHandler(
        cgImage: cgImage,
        orientation: ReceiptOcrModule.orientation(of: image),
        options: [:]
      )
      try handler.perform([request])

      let observations = request.results ?? []
      return observations.compactMap { observation -> [String: Any]? in
        guard let candidate = observation.topCandidates(1).first else { return nil }
        let box = observation.boundingBox
        return [
          "text": candidate.string,
          "x": Double(box.minX),
          "y": Double(1 - box.maxY),
          "width": Double(box.width),
          "height": Double(box.height),
        ]
      }
    }
  }

  /// Camera photos are often stored sideways with a rotation flag; Vision needs that flag.
  private static func orientation(of image: UIImage) -> CGImagePropertyOrientation {
    switch image.imageOrientation {
    case .up: return .up
    case .down: return .down
    case .left: return .left
    case .right: return .right
    case .upMirrored: return .upMirrored
    case .downMirrored: return .downMirrored
    case .leftMirrored: return .leftMirrored
    case .rightMirrored: return .rightMirrored
    @unknown default: return .up
    }
  }
}

internal final class ImageReadException: Exception {
  override var reason: String {
    "Could not read the photo."
  }
}
