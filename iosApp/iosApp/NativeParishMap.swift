import SwiftUI
import MapKit
import ComposeApp

struct ParishMapData: Codable {
    let id: String
    let lat: Double
    let lng: Double
    let title: String
    let subtitle: String
    let hasExtras: Bool
    let hasCandles: Bool
    let glyphText: String
}

class FastSwiftAnnotation: MKPointAnnotation {
    var parishId: String = ""
    var hasExtras: Bool = false
    var hasCandles: Bool = false
    var glyph: String = ""
}

func drawPixelEmoji(_ text: String, size: CGFloat) -> UIImage {
    let format = UIGraphicsImageRendererFormat()
    format.opaque = false
    let renderer = UIGraphicsImageRenderer(size: CGSize(width: size * 1.5, height: size * 1.5), format: format)
    return renderer.image { _ in
        let attrs: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: size)]
        text.draw(at: CGPoint(x: 0, y: 0), withAttributes: attrs)
    }
}

let imgChurch = drawPixelEmoji("⛪", size: 40)
let imgCathedral = drawPixelEmoji("🕍", size: 44)
let imgCrown = drawPixelEmoji("👑", size: 44)

class FlatParishView: MKAnnotationView {
    let pillLabel = UILabel()
    
    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        self.clusteringIdentifier = "parish_cluster"
        self.canShowCallout = true
        self.displayPriority = .defaultLow
        self.isAccessibilityElement = false
        self.accessibilityElementsHidden = true
        if #available(iOS 11.0, *) { self.collisionMode = .none }
        
        pillLabel.font = UIFont.systemFont(ofSize: 11, weight: .bold)
        pillLabel.backgroundColor = UIColor(white: 0.1, alpha: 0.8)
        pillLabel.textColor = .white
        pillLabel.layer.cornerRadius = 6
        pillLabel.layer.masksToBounds = true
        pillLabel.textAlignment = .center
        pillLabel.isHidden = true
        addSubview(pillLabel)
    }
    
    required init?(coder aDecoder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        if !pillLabel.isHidden {
            pillLabel.sizeToFit()
            pillLabel.center = CGPoint(x: bounds.width / 2, y: -14)
        }
    }
}

class ParishClusterView: MKMarkerAnnotationView {
    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        self.displayPriority = .defaultHigh
        self.markerTintColor = .systemBlue
        self.isAccessibilityElement = false
        self.accessibilityElementsHidden = true
        if #available(iOS 11.0, *) { self.collisionMode = .none }
    }
    required init?(coder aDecoder: NSCoder) { fatalError("init(coder:) has not been implemented") }
}

@objc class NativeMapControllerImpl: NSObject, SwiftMapController, MKMapViewDelegate {
    let mapView = MKMapView()
    var onClick: ((String) -> Void)?
    var onCameraChange: ((String) -> Void)?

    var onMapLongClick: ((KotlinDouble, KotlinDouble) -> Void)?

    override init() {
        super.init()
        mapView.delegate = self
        mapView.showsBuildings = false
        mapView.pointOfInterestFilter = .excludingAll
        mapView.isAccessibilityElement = false
        mapView.accessibilityElementsHidden = true

        mapView.register(FlatParishView.self, forAnnotationViewWithReuseIdentifier: "FlatParishView")
        mapView.register(ParishClusterView.self, forAnnotationViewWithReuseIdentifier: "ClusterView")

        let polandCenter = CLLocationCoordinate2D(latitude: 52.0693, longitude: 19.4803)
        let region = MKCoordinateRegion(center: polandCenter, latitudinalMeters: 700000, longitudinalMeters: 700000)
        mapView.setRegion(region, animated: false)

        // 🔥 NOWE: Konfiguracja i dodanie gestu długiego przytrzymania do mapy
        let longPressGesture = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
        longPressGesture.minimumPressDuration = 0.5 // Pół sekundy przytrzymania wywoła akcję
        mapView.addGestureRecognizer(longPressGesture)
    }

    var view: UIView { return mapView }

    func setMapTheme(isDark: Bool) {
        if isDark {
            mapView.overrideUserInterfaceStyle = .dark
        } else {
            mapView.overrideUserInterfaceStyle = .light
        }
    }

    func setOnMarkerClickListener(onClick: @escaping (String) -> Void) {
        self.onClick = onClick
    }

    func setOnCameraChangeListener(onChange: @escaping (String) -> Void) {
        self.onCameraChange = onChange
        self.mapView(self.mapView, regionDidChangeAnimated: false)
    }

    // Zwróć uwagę na KotlinDouble zamiast Double
    func setOnMapLongClickListener(onLongClick: @escaping (KotlinDouble, KotlinDouble) -> Void) {
        self.onMapLongClick = onLongClick
    }

    @objc func handleLongPress(_ gestureRecognizer: UILongPressGestureRecognizer) {
        if gestureRecognizer.state == .began {
            let touchPoint = gestureRecognizer.location(in: mapView)
            let coordinate = mapView.convert(touchPoint, toCoordinateFrom: mapView)

            onMapLongClick?(
                KotlinDouble(value: coordinate.latitude),
                KotlinDouble(value: coordinate.longitude)
            )
        }
    }

    func centerOn(lat: Double, lng: Double) {
        let coord = CLLocationCoordinate2D(latitude: lat, longitude: lng)
        let region = MKCoordinateRegion(center: coord, latitudinalMeters: 2000, longitudinalMeters: 2000)
        mapView.setRegion(region, animated: true)
    }

    func updateParishes(jsonString: String) {
        DispatchQueue.global(qos: .userInitiated).async {
            guard let data = jsonString.data(using: .utf8) else { return }
            do {
                let parishes = try JSONDecoder().decode([ParishMapData].self, from: data)

                let newAnnotations = parishes.map { p -> FastSwiftAnnotation in
                    let ann = FastSwiftAnnotation()
                    ann.coordinate = CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng)
                    ann.title = p.title
                    if !p.subtitle.isEmpty { ann.subtitle = p.subtitle }
                    ann.parishId = p.id
                    ann.hasExtras = p.hasExtras
                    ann.hasCandles = p.hasCandles
                    ann.glyph = p.glyphText
                    return ann
                }

                DispatchQueue.main.async {
                    let current = self.mapView.annotations.compactMap { $0 as? FastSwiftAnnotation }
                    let currentIds = Set(current.map { $0.parishId })
                    let newIds = Set(newAnnotations.map { $0.parishId })

                    let toRemove = current.filter { !newIds.contains($0.parishId) }
                    let toAdd = newAnnotations.filter { !currentIds.contains($0.parishId) }

                    if !toRemove.isEmpty { self.mapView.removeAnnotations(toRemove) }
                    if !toAdd.isEmpty { self.mapView.addAnnotations(toAdd) }
                }
            } catch {
                print("Błąd parsowania JSON: \(error)")
            }
        }
    }

    func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
        let center = mapView.region.center
        let span = mapView.region.span

        let latDelta = span.latitudeDelta * 1.5
        let lngDelta = span.longitudeDelta * 1.5

        let minLat = center.latitude - latDelta / 2
        let maxLat = center.latitude + latDelta / 2
        let minLng = center.longitude - lngDelta / 2
        let maxLng = center.longitude + lngDelta / 2

        onCameraChange?("\(minLat),\(maxLat),\(minLng),\(maxLng)")
    }

    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        if annotation is MKUserLocation { return nil }
        if let cluster = annotation as? MKClusterAnnotation {
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: "ClusterView", for: annotation) as! ParishClusterView
            return view
        }
        if let pAnn = annotation as? FastSwiftAnnotation {
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: "FlatParishView", for: annotation) as! FlatParishView

            if pAnn.glyph == "👑" { view.image = imgCrown }
            else if pAnn.glyph == "🕍" { view.image = imgCathedral }
            else { view.image = imgChurch }

            if pAnn.hasExtras, let subtitle = pAnn.subtitle, !subtitle.isEmpty {
                view.pillLabel.text = "  \(subtitle)  "
                view.pillLabel.isHidden = false
                view.setNeedsLayout()
            } else {
                view.pillLabel.isHidden = true
            }

            if pAnn.hasCandles {
                view.layer.shadowColor = UIColor.systemOrange.cgColor
                view.layer.shadowOffset = .zero
                view.layer.masksToBounds = false
                view.layer.shadowRadius = 8.0
                view.layer.shadowOpacity = 1.0

                let glowRect = view.bounds.insetBy(dx: -6, dy: -6)
                view.layer.shadowPath = UIBezierPath(ovalIn: glowRect).cgPath

                view.layer.shouldRasterize = true
                view.layer.rasterizationScale = UIScreen.main.scale

            } else {
                view.layer.shadowOpacity = 0.0
                view.layer.shadowPath = nil
                view.layer.shouldRasterize = false
            }

            return view
        }
        return nil
    }

    func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
        if let pAnn = view.annotation as? FastSwiftAnnotation {
            onClick?(pAnn.parishId)
            mapView.deselectAnnotation(pAnn, animated: false)
        }
    }
}
@objc class NativeMapFactoryImpl: NSObject, SwiftMapFactory {
    func createMap() -> SwiftMapController {
        return NativeMapControllerImpl()
    }
}