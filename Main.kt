package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt

data class pixData(val coord: Pair<Int, Int> = Pair(0, 0), val energy: Double = 0.0, var vEnergy: Double = 0.0, val hEnergy: Double = 0.0)

fun grad2(axis: String, x: Int, y: Int, image: BufferedImage): Double {
    val pixel1 = Color(image.getRGB(if (axis == "x") x - 1 else x, if (axis == "y") y - 1 else y))
    val pixel2 = Color(image.getRGB(if (axis == "x") x + 1 else x, if (axis == "y") y + 1 else y))
    val rDiff = pixel2.red - pixel1.red
    val gDiff = pixel2.green - pixel1.green
    val bDiff = pixel2.blue - pixel1.blue
    return (rDiff * rDiff + gDiff * gDiff + bDiff * bDiff).toDouble()
}

fun energy(x: Int, y: Int, im: BufferedImage): Double {
    return sqrt(grad2("x", x.coerceIn(1, im.width - 2), y, im) + grad2("y", x, y.coerceIn(1, im.height - 2), im))
}

fun createEnergyMatrix(originalImage: BufferedImage): List<MutableList<pixData>> {
    val energyMatrix = List(originalImage.width) { MutableList(originalImage.height) { pixData() } }
    for (x in 0 until originalImage.width) {
        for (y in 0 until originalImage.height) {
            val energy = energy(x, y, originalImage)
            val hEnergy = if (x == 0) energy else (List(3) { energyMatrix[x-1][(y-1+it).coerceIn(0, energyMatrix[0].lastIndex)].hEnergy }.minOrNull() ?: 0.0) + energy
            energyMatrix[x][y] = pixData(Pair(x, y), energy(x, y, originalImage), 0.0, hEnergy)
        }
    }
    for (y in 0 until originalImage.height) {
        for (x in 0 until originalImage.width) {
            val vEnergy = if (y == 0) energyMatrix[x][y].energy else (List(3) { energyMatrix[(x-1+it).coerceIn(0, energyMatrix.lastIndex)][y-1].vEnergy }.minOrNull() ?: 0.0) + energyMatrix[x][y].energy
            energyMatrix[x][y].vEnergy = vEnergy
        }
    }
    return energyMatrix
}

fun seamFinder(energyMatrix: List<MutableList<pixData>>, direction: String): MutableList<pixData> {
    if (direction == "width") {
        val seam = mutableListOf(List(energyMatrix.size) { energyMatrix[it][energyMatrix[0].size - 1] }.minByOrNull { it.vEnergy } ?: pixData())
        for (y in energyMatrix[0].size - 2 downTo 0) {
            seam.add(List(3) { energyMatrix[(seam.last().coord.first - 1 + it).coerceIn(0, energyMatrix.lastIndex)][y] }.minByOrNull { it.vEnergy } ?: pixData())
        }
        return seam
    } else {
        val seam = mutableListOf(List(energyMatrix[0].size) { energyMatrix[energyMatrix.size - 1][it] }.minByOrNull { it.hEnergy } ?: pixData())
        for (x in energyMatrix.size - 2 downTo 0) {
            seam.add(List(3) { energyMatrix[x][(seam.last().coord.second - 1 + it).coerceIn(0, energyMatrix[0].lastIndex)] }.minByOrNull { it.hEnergy } ?: pixData())
        }
        return seam
    }
}

fun reduce(direction: String, amount: Int, originalImage: BufferedImage): BufferedImage {
    var image = originalImage
    repeat(amount) {
        val seam = seamFinder(createEnergyMatrix(image), direction)
        if (direction == "width") {
            val newImage = BufferedImage(image.width - 1, image.height, BufferedImage.TYPE_INT_RGB)
            for (pixel in seam) {
                for (x in 0 until pixel.coord.first) {
                    newImage.setRGB(x, pixel.coord.second, image.getRGB(x, pixel.coord.second))
                }
                for (x in pixel.coord.first until newImage.width) {
                    newImage.setRGB(x, pixel.coord.second, image.getRGB(x + 1, pixel.coord.second))
                }
            }
            image = newImage
        } else {
            val newImage = BufferedImage(image.width, image.height - 1, BufferedImage.TYPE_INT_RGB)
            for (pixel in seam) {
                for (y in 0 until pixel.coord.second) {
                    newImage.setRGB(pixel.coord.first, y, image.getRGB(pixel.coord.first, y))
                }
                for (y in pixel.coord.second until newImage.height) {
                    newImage.setRGB(pixel.coord.first, y, image.getRGB(pixel.coord.first, y + 1))
                }
            }
            image = newImage
        }
    }
    return image
}

fun main(args: Array<String>) {
    val image: BufferedImage = ImageIO.read(File(args[args.indexOf("-in") + 1]))
    val reduceWidth = args[args.indexOf("-width") + 1].toInt()
    val reduceHeight = args[args.indexOf("-height") + 1].toInt()
    val reducedImage = reduce("height", reduceHeight, reduce("width", reduceWidth, image))
    ImageIO.write(reducedImage, "png", File(args[args.indexOf("-out") + 1]))
}