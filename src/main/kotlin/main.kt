/**
 * Runs the Pencil Sketching Algorithm
 */

fun main() {
    val filename = "Images/tree.jpg"

    val (image, width, height) = readImage(filename)

    val gaussianSize = 31
    val sigma = 6.0
    val bilateralSize = 3
    val sigmaC = 230.0
    val sigmaS = 230.0
    val gamma = 6.0

    val gray = image.clone()
    grayscale(gray)
    equalizeHistogram(gray)

    val sketch = gray.clone()

    gaussian(sketch, width, height, gaussianSize, sigma)
    blend(gray, sketch)
    bilateral(sketch, width, height, bilateralSize, sigmaC, sigmaS)
    gamma(sketch, gamma)

    // Colored Pencil Sketch
    coloredPencil(image, sketch,1.0, 0.8)
    writeImage(image, width, height, "colored.png")

    RGB(sketch)
    // Black and White Sketch
    writeImage(sketch, width, height, "result.png")
}