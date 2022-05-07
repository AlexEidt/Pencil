/**
 * Runs the Pencil Sketching Algorithm
 */

fun main() {
    val filename = "Results/rocks.jpg"

    val (image, width, height) = readImage(filename)

    val gaussianSize = 31
    val sigma = 6.0
    val bilateralSize = 3
    val sigmaC = 230.0
    val sigmaS = 230.0
    val gamma = 6.0
    val hue = 1.0
    val saturation = 0.8

    val gray = image.clone()
    grayscale(gray)
    equalizeHistogram(gray)

    val sketch = gray.clone()

    gaussian(sketch, width, height, gaussianSize, sigma)

    blend(gray, sketch)

    bilateral(sketch, width, height, bilateralSize, sigmaC, sigmaS)
    gamma(sketch, gamma)

    // Colored Pencil Sketch
    coloredPencil(image, sketch, hue, saturation)
    writeImage(image, width, height, "${filename.split(".")[0]}_colored1.jpg")

    RGB(sketch)
    // Black and White Sketch
    writeImage(sketch, width, height, "${filename.split(".")[0]}_result1.jpg")
}