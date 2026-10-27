import AppKit
import CoreGraphics

// Draws the TVRenamer icon artwork on Apple's 1024px macOS grid, where the
// body is 824px with 100px of clear margin. The cabinet fills the whole body,
// so once build-icns.sh cuts it to Apple's outline there is nothing for macOS
// to sit on a grey tile. Usage: swift draw.swift out.png

let size = 1024
let cs = CGColorSpace(name: CGColorSpace.sRGB)!
let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
                    space: cs, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
// Work top-down like a design tool.
ctx.translateBy(x: 0, y: CGFloat(size))
ctx.scaleBy(x: 1, y: -1)

func rgb(_ hex: UInt32, _ a: CGFloat = 1) -> CGColor {
    CGColor(srgbRed: CGFloat((hex >> 16) & 0xff) / 255, green: CGFloat((hex >> 8) & 0xff) / 255,
            blue: CGFloat(hex & 0xff) / 255, alpha: a)
}

func gradient(_ stops: [(CGFloat, CGColor)]) -> CGGradient {
    CGGradient(colorsSpace: cs, colors: stops.map { $0.1 } as CFArray, locations: stops.map { $0.0 })!
}

// Superellipse |x|^n + |y|^n = 1, used for the bulging glass of the tube.
func squircle(_ r: CGRect, n: CGFloat) -> CGPath {
    let p = CGMutablePath()
    let steps = 720
    for i in 0...steps {
        let t = CGFloat(i) / CGFloat(steps) * 2 * .pi
        let c = cos(t), s = sin(t)
        let x = pow(abs(c), 2 / n) * (c < 0 ? -1 : 1)
        let y = pow(abs(s), 2 / n) * (s < 0 ? -1 : 1)
        let pt = CGPoint(x: r.midX + x * r.width / 2, y: r.midY + y * r.height / 2)
        i == 0 ? p.move(to: pt) : p.addLine(to: pt)
    }
    p.closeSubpath()
    return p
}

// Deterministic noise so the wood grain is the same on every run.
var seed: UInt64 = 0x7652_6e6d
func rand() -> CGFloat {
    seed = seed &* 6364136223846793005 &+ 1442695040888963407
    return CGFloat(seed >> 33) / CGFloat(1 << 31)
}

// MARK: Cabinet

let bodyRect = CGRect(x: 100, y: 100, width: 824, height: 824)
// Overdraw past the body. The exact outline comes from Apple's mask later, and
// macOS adds the rim and shadow itself.
let body = CGPath(rect: bodyRect.insetBy(dx: -8, dy: -8), transform: nil)

ctx.saveGState()
ctx.addPath(body)
ctx.clip()
ctx.drawLinearGradient(gradient([(0, rgb(0xB57834)), (0.55, rgb(0x8C5223)), (1, rgb(0x5E3313))]),
                       start: CGPoint(x: 0, y: 100), end: CGPoint(x: 0, y: 924), options: [])

// Grain: long, slightly wavy horizontal strokes in darker and lighter browns.
for _ in 0..<420 {
    let y0 = 90 + rand() * 850
    let amp = 2 + rand() * 7
    let freq = 0.004 + rand() * 0.01
    let phase = rand() * 6.28
    let line = CGMutablePath()
    var x: CGFloat = 80
    line.move(to: CGPoint(x: x, y: y0 + amp * sin(phase + x * freq)))
    while x < 950 {
        x += 8
        let wobble = (rand() - 0.5) * 0.8
        line.addLine(to: CGPoint(x: x, y: y0 + amp * sin(phase + x * freq) + wobble))
    }
    let dark = rand() < 0.7
    ctx.setStrokeColor(dark ? rgb(0x3A1C08, 0.06 + rand() * 0.18) : rgb(0xE0A060, 0.05 + rand() * 0.12))
    ctx.setLineWidth(0.6 + rand() * 2.2)
    ctx.addPath(line)
    ctx.strokePath()
}

// Light from above: a soft sheen across the top of the cabinet.
ctx.drawLinearGradient(gradient([(0, rgb(0xFFFFFF, 0.22)), (0.35, rgb(0xFFFFFF, 0)),
                                 (0.8, rgb(0x000000, 0)), (1, rgb(0x000000, 0.25))]),
                       start: CGPoint(x: 0, y: 100), end: CGPoint(x: 0, y: 924), options: [])

ctx.restoreGState()

// MARK: Chassis, the dark recess the tube sits in

let chassisRect = bodyRect.insetBy(dx: 92, dy: 92)
let chassis = CGPath(roundedRect: chassisRect, cornerWidth: 56, cornerHeight: 56, transform: nil)

// Lip where the wood meets the recess.
ctx.saveGState()
ctx.addPath(CGPath(roundedRect: chassisRect.insetBy(dx: -10, dy: -10), cornerWidth: 64, cornerHeight: 64,
                   transform: nil))
ctx.setFillColor(rgb(0x3B1E0A, 0.85))
ctx.fillPath()
ctx.restoreGState()

ctx.saveGState()
ctx.addPath(chassis)
ctx.clip()
ctx.drawLinearGradient(gradient([(0, rgb(0x0A0A0B)), (1, rgb(0x2B2C2F))]),
                       start: CGPoint(x: 0, y: chassisRect.minY), end: CGPoint(x: 0, y: chassisRect.maxY),
                       options: [])
// Recessed, so the top edge casts a shadow inward.
ctx.setShadow(offset: CGSize(width: 0, height: 14), blur: 30, color: rgb(0x000000, 0.9))
ctx.addPath(CGPath(rect: chassisRect.insetBy(dx: -60, dy: -60), transform: nil))
ctx.addPath(chassis)
ctx.setFillColor(rgb(0x000000))
ctx.fillPath(using: .evenOdd)
ctx.restoreGState()

// MARK: Tube

// A 4:3-ish tube, like the original artwork.
let screenRect = CGRect(x: chassisRect.minX + 44, y: chassisRect.minY + 100,
                        width: chassisRect.width - 88, height: chassisRect.height - 200)
let screen = squircle(screenRect, n: 3.6)

// Blue glow the tube throws on the chassis.
ctx.saveGState()
ctx.addPath(chassis)
ctx.clip()
ctx.setShadow(offset: .zero, blur: 60, color: rgb(0x3FA6FF, 0.55))
ctx.addPath(screen)
ctx.setFillColor(rgb(0x1B5FA8))
ctx.fillPath()
ctx.restoreGState()

// Black bezel ring.
ctx.saveGState()
ctx.addPath(squircle(screenRect.insetBy(dx: -12, dy: -12), n: 3.6))
ctx.setFillColor(rgb(0x050506))
ctx.fillPath()
ctx.restoreGState()

ctx.saveGState()
ctx.addPath(screen)
ctx.clip()
ctx.drawRadialGradient(gradient([(0, rgb(0xE4F7FF)), (0.28, rgb(0x7CC7EE)), (0.62, rgb(0x2A7CC4)),
                                 (1, rgb(0x0B3470))]),
                       startCenter: CGPoint(x: screenRect.midX, y: screenRect.minY + 60), startRadius: 0,
                       endCenter: CGPoint(x: screenRect.midX, y: screenRect.minY + 60),
                       endRadius: screenRect.height * 1.05, options: [.drawsAfterEndLocation])
// Scanlines.
var y = screenRect.minY
while y < screenRect.maxY {
    ctx.setFillColor(rgb(0x062448, 0.11))
    ctx.fill(CGRect(x: screenRect.minX, y: y, width: screenRect.width, height: 2.5))
    y += 6
}
// Curved glass: darker toward the edges.
ctx.drawRadialGradient(gradient([(0.55, rgb(0x000000, 0)), (1, rgb(0x00112A, 0.55))]),
                       startCenter: CGPoint(x: screenRect.midX, y: screenRect.midY), startRadius: 0,
                       endCenter: CGPoint(x: screenRect.midX, y: screenRect.midY),
                       endRadius: screenRect.width * 0.62, options: [.drawsAfterEndLocation])
ctx.restoreGState()

// Glossy reflection across the top of the glass.
ctx.saveGState()
ctx.addPath(screen)
ctx.clip()
let gloss = CGMutablePath()
gloss.addEllipse(in: CGRect(x: screenRect.minX - 120, y: screenRect.minY - screenRect.height * 0.62,
                            width: screenRect.width + 240, height: screenRect.height * 1.02))
ctx.addPath(gloss)
ctx.clip()
ctx.drawLinearGradient(gradient([(0, rgb(0xFFFFFF, 0.36)), (1, rgb(0xFFFFFF, 0))]),
                       start: CGPoint(x: 0, y: screenRect.minY), end: CGPoint(x: 0, y: screenRect.midY),
                       options: [])
ctx.restoreGState()

// Thin highlight on the bottom edge of the glass.
ctx.saveGState()
ctx.addPath(screen)
ctx.clip()
ctx.translateBy(x: 0, y: -5)
ctx.addPath(screen)
ctx.setLineWidth(5)
ctx.setStrokeColor(rgb(0x9ED8FF, 0.45))
ctx.strokePath()
ctx.restoreGState()

let out = URL(fileURLWithPath: CommandLine.arguments[1])
let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
try! rep.representation(using: .png, properties: [:])!.write(to: out)
