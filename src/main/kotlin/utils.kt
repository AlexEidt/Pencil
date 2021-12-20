/**
 * Contains some useful and unrelated functions for the Pencil Sketcher.
 */

import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.system.exitProcess

// Three-way maximum.
fun max(a: Double, b: Double, c: Double): Double {
    return if (a > b) if (a > c) a else c else if (b > c) b else c
}

// Three-way minimum.
fun min(a: Double, b: Double, c: Double): Double {
    return if (a < b) if (a < c) a else c else if (b < c) b else c
}

/*
 * Edge pads an image.
 *
 * @param result    The image to write to.
 * @param image     The image to pad.
 * @param width     The width of the image.
 * @param height    The height of the image.
 * @param size      The width of the padding along with edges.
 * @return          The padded image.
 */
fun pad(result: IntArray, image: IntArray, width: Int, height: Int, size: Int) {
    val pad = size / 2
    var index = 0
    for (h in 0 until pad) {
        for (w in 0 until pad) result[index++] = image[0]
        for (w in pad until width + pad) result[index++] = image[w - pad]
        for (w in width + pad until width + size) result[index++] = image[width - 1]
    }
    for (h in pad until height + pad) {
        for (w in 0 until pad) result[index++] = image[(h - pad) * width]
        for (w in pad until width + pad) result[index++] = image[(h - pad) * width + w - pad]
        for (w in width + pad until width + size) result[index++] = image[(h - pad) * width + width - 1]
    }
    for (h in height + pad until height + size) {
        for (w in 0 until pad) result[index++] = image[(height - 1) * width]
        for (w in pad until width + pad) result[index++] = image[(height - 1) * width + w - pad]
        for (w in width + pad until width + size) result[index++] = image[(height - 1) * width + width - 1]
    }
}

/*
 * Converts an integer array representing pixels to a BufferedImage, which is needed
 * to display the image on the User Interface.
 *
 * @param image     The image. Each int represents an RGB pixel.
 * @param width     The width of the image.
 * @param height    The height of the image.
 * @return          The BufferedImage of the input image.
 */
fun bufferImage(image: IntArray, width: Int, height: Int): BufferedImage {
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    bufferedImage.setRGB(0, 0, width, height, image, 0, width)
    return bufferedImage
}

/*
 * Writes the given integer array to an image.
 *
 * @param image         Image as an integer array. Each int represents an RGB pixel.
 * @param width         The width of the image.
 * @param height        The height of the image.
 * @param filename      Image file name.
 * @throws              IOException if the given image could not be created.
 */
@Throws(IOException::class)
fun writeImage(image: IntArray, width: Int, height: Int, filename: String) {
    val file = File(filename)
    val bufferedImage = bufferImage(image, width, height)
    ImageIO.write(bufferedImage, file.extension, file)
}

/*
 * Reads an image into a 2D Integer array, where each int represents an RGB pixel.
 *
 * @param filename      Image file name to read.
 * @return              Image as Integer array along with the image width and height.
 */
fun readImage(filename: String): Triple<IntArray, Int, Int> {
    return try {
        val image = ImageIO.read(File(filename))
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        var index = 0
        for (h in 0 until height) {
            for (w in 0 until width) {
                pixels[index++] = image.getRGB(w, h)
            }
        }
        Triple(pixels, width, height)
    } catch (e: IOException) {
        println("Error opening $filename")
        println(e.message)
        exitProcess(1)
    }
}