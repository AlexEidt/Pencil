/**
 * Contains functions for running the Image Processing Pipeline
 * to create Pencil Sketches.
 */

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/*
 * Grayscales the image in place.
 *
 * @param image     The image to grayscale. Each int represents an RGB pixel.
 * @param width     The width of the image.
 * @param height    The height of the image.
 */
fun grayscale(image: IntArray) {
    parallel { cpu, cpus ->
        for (i in cpu until image.size step cpus) {
            val pixel = image[i]
            val r = pixel shr 16 and 0xFF
            val g = pixel shr 8 and 0xFF
            val b = pixel and 0xFF
            image[i] = (3 * r + 4 * g + b) / 8
        }
    }
}

/*
 * Performs Histogram Equalization on the Image.
 *
 * @param image     The image to grayscale. Each int represents an RGB pixel.
 */
fun equalizeHistogram(image: IntArray) {
    val cpus = Runtime.getRuntime().availableProcessors()

    val histogram = IntArray(256) { 0 }
    val histograms = Array(cpus) { IntArray(256) { 0 } }

    parallel { cpu, cpus ->
        for (i in cpu until image.size step cpus) {
            histograms[cpu][image[i]]++
        }
    }

    histograms.forEach { hist -> hist.forEachIndexed { i, count -> histogram[i] += count } }

    // Cumulative Sum of Histogram.
    (1 until histogram.size).forEach { histogram[it] += histogram[it - 1] }
    // Cumulative Distribution Function (CDF) of Histogram.
    histogram.forEachIndexed { index, value -> histogram[index] = (255.0 * value / histogram[255] + 0.5).toInt() }
    image.forEachIndexed { index, pixel -> image[index] = histogram[pixel] }
}

/*
 * Creates a Gaussian Filter of length "size".
 *
 * @param size      The size of the Gaussian Filter.
 * @param sigma     Parameter for Gaussian Function.
 */
fun gaussian1D(size: Int, sigma: Double): DoubleArray {
    var n = size
    if (size % 2 == 0) n++

    val variance = sigma * sigma * 2.0
    val coefficient = 1 / sqrt(variance * Math.PI)

    val vector = DoubleArray(n) {
        val x = it - n / 2
        coefficient * exp(-(x * x) / variance)
    }

    return vector.map { it / vector.sum() }.toDoubleArray()
}

/*
 * Creates a 2D Gaussian Filter of dimension "size x size".
 *
 * @param size      The size of one side of the filter.
 * @param sigma     Parameter for Gaussian Function.
 */
fun gaussian2D(size: Int, sigma: Double): Array<DoubleArray> {
    var n = size
    if (size % 2 == 0) n++

    val variance = sigma * sigma * 2.0
    val coefficient = 1 / (variance * Math.PI)

    val vector = Array(n) { it ->
        val x = it - n / 2
        DoubleArray(n) {
            val y = it - n / 2
            coefficient * exp(-(x * x + y * y) / variance)
        }
    }

    val sum = vector.sumOf { it.sum() }
    return vector.map { it -> it.map { it / sum }.toDoubleArray() }.toTypedArray()
}

/*
 * Performs Gaussian Smoothing.
 *
 * @param sketch    Final modified image.
 * @param width     Width of Image.
 * @param height    Height of Image.
 * @param size      The size of the Gaussian Kernel to use.
 * @param sigma     Parameter for the Gaussian Function.
 */
fun gaussian(sketch: IntArray, width: Int, height: Int, size: Int, sigma: Double) {
    if (size == 0) return
    val half = size / 2
    val padded = IntArray((width + size) * (height + size))

    val gauss = gaussian1D(size, sigma)
    // Edge Pad "image" by "half" and store in "padded".
    pad(padded, sketch, width, height, size)
    parallel { cpu, cpus ->
        for (h in cpu until height step cpus) {
            for (w in 0 until width) {
                var sum = 0.5
                for (i in 0 until size) {
                    sum += gauss[i] * padded[(h + half) * (width + size) + w + i]
                }
                sketch[h * width + w] = sum.toInt()
            }
        }
    }
    pad(padded, sketch, width, height, size)
    parallel { cpu, cpus ->
        for (h in cpu until height step cpus) {
            for (w in 0 until width) {
                var sum = 0.5
                for (i in 0 until size) {
                    sum += gauss[i] * padded[(h + i) * (width + size) + w + half]
                }
                sketch[h * width + w] = sum.toInt()
            }
        }
    }
}

/*
 * Performs a Bilateral Filter.
 *
 * @param sketch    The final sketched image.
 * @param width     The width of the image.
 * @param height    The height of the image.
 * @param size      The size of the Gaussian Kernel to use.
 * @param sigmaC    Sigma for Color.
 * @param sigmaS    Sigma for Space.
 */
fun bilateral(sketch: IntArray, width: Int, height: Int, size: Int, sigmaC: Double, sigmaS: Double) {
    // Edge Pad "image" by "half" and store in "padded".
    val padded = IntArray((width + size) * (height + size))
    pad(padded, sketch, width, height, size)

    val gaussS = gaussian2D(size, sigmaS)
    val variance = sigmaC * sigmaC * 2.0
    val coefficient = 1 / sqrt(variance * Math.PI)
    val gaussC = DoubleArray(256) { coefficient * exp(-(it * it) / variance) }

    parallel { cpu, cpus ->
        for (h in cpu until height step cpus) {
            for (w in 0 until width) {
                val index = h * width + w
                val current = sketch[index]
                var sum = 0.0
                var total = 0.0
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        val pixel = padded[(h + i) * (width + size) + w + j]
                        val s = gaussS[i][j]
                        val c = gaussC[abs(current - pixel)]
                        val weight = s * c
                        sum += weight
                        total += pixel * weight
                    }
                }
                sketch[index] = (total / sum + 0.5).toInt()
            }
        }
    }
}

/*
 * Blends the grayscaled and blurred images together to create the pencil
 * sketch effect. Stores result in sketch.
 *
 * @param gray      The grayscaled image.
 * @param sketch    The final sketch image.
 */
fun blend(gray: IntArray, sketch: IntArray) {
    parallel { cpu, cpus ->
        for (index in cpu until sketch.size step cpus) {
            val pixel = sketch[index]
            if (pixel == 0) {
                sketch[index] = 255
            } else {
                var blended = gray[index] * 256 / pixel
                if (blended > 255) blended = 255
                sketch[index] = blended
            }
        }
    }
}

/*
 * Gamma corrects the sketch to adjust brightness.
 *
 * @param image     The final sketched image.
 * @param gamma     Gamma value to gamma correct by.
 */
fun gamma(image: IntArray, gamma: Double) {
    val precalculated = (0..255).map {
        ((it / 255.0).pow(gamma) * 255.0 + 0.5).toInt()
    }.toList()

    parallel { cpu, cpus ->
        for (index in cpu until image.size step cpus) {
            image[index] = precalculated[image[index]]
        }
    }
}

/*
 * Converts the sketch to a colored-pencil like image:
 *      1. First converts the original image to HSV.
 *      2. Scales the Hue and Saturation by the given parameters.
 *      3. Replaces the values with the sketched image.
 *      4. Converts back to RGB.
 *
 * @param image         The original image.
 * @param sketch        The final sketched image.
 * @param hue           The scaling factor to use for the hue.
 * @param saturation    The scaling factor to use for the saturation.
 */
fun coloredPencil(image: IntArray, sketch: IntArray, hue: Double, saturation: Double) {
    // Convert image to HSV for colored pencil rendering.
    val hsv = DoubleArray(image.size * 3)
    RGBHSV(image, hsv)

    parallel { cpu, cpus ->
        for (i in cpu until sketch.size step cpus) {
            val pixel = sketch[i]
            val index = i * 3
            hsv[index+0] *= hue
            hsv[index+1] = hsv[index+1].pow(saturation)
            hsv[index+2] = pixel / 255.0
        }
    }

    HSVRGB(image, hsv)
}

/*
 * Converts a grayscale image to RGB format by copying the
 * gray value into each RGB slot.
 * 
 * @param image     The image to convert.
 */
fun RGB(image: IntArray) {
    parallel { cpu, cpus ->
        for (index in cpu until image.size step cpus) {
            val pixel = image[index]
            image[index] = (pixel shl 16) or (pixel shl 8) or pixel
        }
    }
}

/*
 * Converts an RGB image to HSV.
 *
 * @param image     The image to convert.
 * @param hsv       The destination array to store HSV values in.
 */
fun RGBHSV(image: IntArray, hsv: DoubleArray) {
    parallel { cpu, cpus ->
        for (i in cpu until image.size step cpus) {
            val pixel = image[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0
            val g = ((pixel shr 8) and 0xFF) / 255.0
            val b = (pixel and 0xFF) / 255.0

            val value = max(r, g, b)
            val min = min(r, g, b)
            val c = value - min

            val saturation = if (value == 0.0) 0.0 else c / value

            var hue = if (c == 0.0) 0.0
            else if (value == r) (g - b) / c
            else if (value == g) (b - r) / c + 2.0
            else (r - g) / c + 4.0

            hue = if (hue < 0.0) hue / 6.0 + 1.0 else hue / 6.0

            val index = i * 3
            hsv[index+0] = hue
            hsv[index+1] = saturation
            hsv[index+2] = value
        }
    }
}

/*
 * Converts an RGB image to HSV.
 *
 * @param image     The destination image.
 * @param hsv       The array holding HSV values.
 */
fun HSVRGB(image: IntArray, hsv: DoubleArray) {
    parallel { cpu, cpus ->
        for (i in cpu until image.size step cpus) {
            val index = i * 3
            val h = hsv[index+0] * 360.0
            val s = hsv[index+1]
            val v = hsv[index+2]

            val c = s * v
            val x = c * (1.0 - abs(h / 60.0 % 2.0 - 1.0))
            val m = v - c

            var R: Double
            var G: Double
            var B: Double

            when {
                h >= 300.0 -> { R = c; G = 0.0; B = x; }
                h >= 240.0 -> { R = x; G = 0.0; B = c; }
                h >= 180.0 -> { R = 0.0; G = x; B = c; }
                h >= 120.0 -> { R = 0.0; G = c; B = x; }
                h >= 60.0 -> { R = x; G = c; B = 0.0; }
                else -> { R = c; G = x; B = 0.0; }
            }

            val r = ((R + m) * 255.0 + 0.5).toInt()
            val g = ((G + m) * 255.0 + 0.5).toInt()
            val b = ((B + m) * 255.0 + 0.5).toInt()

            image[i] = (r shl 16) or (g shl 8) or b
        }
    }
}