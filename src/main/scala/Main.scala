import akka.actor._
import java.io.{File, IOException}

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.Color

// Case classes for messaging
// Message for boss with the path of the image to check
case class Path(path: String)
// Message for worker with the read image + specified settings
case class Photo(photo: BufferedImage, threshold: Int, outPath: String)
// Initial message to boss with program settings
case class Settings(threshold: Int, outPath: String)
// Message to boss when the file is saved properly
case class Finish(filePath: String)

// Boss class which is responsible of providing tasks for workers
class Boss extends Actor {
  // Counter to get track of processed images
  var photosToProcess = 0
  var counterPhotos = 0

  // Initial method to start boss actor and to specify settings
  def receive: Receive = {
    case Settings(threshold, outPath) => context.become(assignJobs(threshold, outPath))
    case Path => throw new RuntimeException("You have to specify the settings first")
  }
  // In this method boss is sending photos to newly created children and notify if the worker finished his job
  def assignJobs(threshold: Int, outPath: String): Receive = {

    // Receives path of an image to check
    case Path(path) =>
      // Opens image from the provided path
      try {
        val photo = ImageIO.read(new File(path))
        // Gets the name of the file to name worker properly
        val name = path.split("\\\\").last
        val worker = context.actorOf(Props[Worker], name = name)
        // Increases counter of photos to process
        photosToProcess += 1
        worker ! Photo(photo, threshold, outPath)
      } catch {
        case exception: IOException => println(s"IOException:\n $exception")
        case exception: Exception => println(s"UndefinedException\n $exception")
      }

    // Notify user when the worker finished his job
    case Finish(filePath) =>
      counterPhotos += 1
      println(s"{ $counterPhotos/$photosToProcess } || $filePath")
      if (counterPhotos == photosToProcess) context.stop(self)
    // Throw exception when boss gets Settings message during the assignJobs state
    case Settings => throw new RuntimeException("Settings are already specified. You can not change them during runtime.")
  }
}

// Worker class used to calculate image luminance values and save pictures to provided folder
class Worker extends Actor {
  def receive: Receive = {
    case Photo(photo, threshold, outPath) =>
      /* Calculates what is the max value of the all white photo. I uses it for reference to calculate
      *  percentage of "whiteness" */
      val maxWhite = calculateMaxWhiteValue(photo)

      // Calculates picture "whiteness" value using Relative luminance formula (ITU-R BT.709)
      val sum = calculatePixels(photo)

      /* Switches the scale to all black
      *  calculates the percentage of the "whiteness" of the image
      *  subtracts it from 1.0 to get the "darkness */
      val darkVal = 1.0 - (sum * 1.0 / maxWhite)

      // Rounds result to get two digits
      val result = BigDecimal(darkVal).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

      /* Gets format of the original file + the path and name to save it
      *  namesToSave is a tuple containing on ._1 - PATH to save image and on ._2 format of original */
      val namesToSave = prepareName(context.self.path.name, threshold, result, outPath)

      try {
        // Saves the file that it was taking care of
        ImageIO.write(photo, namesToSave._2, new File(namesToSave._1))
      } catch {
        case exception: IOException => println(s"IOException:\n $exception")
        case exception: Exception => println(s"UndefinedException:\n $exception")
      }
      // Declares finished job to the boss
      sender() ! Finish(namesToSave._1)

      // Stops working
      context.stop(self)

  }
  // Tailrec function to iterate over every pixel of an image and calculate sum of the values
  def calculatePixels(photo: BufferedImage): Double = {
    @scala.annotation.tailrec
    def helper(accumulator: Double, calcWidth: Int, calcHeight: Int): Double = {
      // Reset height tracker and go to the next column
      if (calcHeight >= photo.getHeight) helper(accumulator, calcWidth + 1, 0)
      else
        // Return accumulator if every column of pixels was summed
        if (calcWidth >= photo.getWidth) accumulator
        else {
          // Get RGB values of pixel
          val pixelColor = new Color(photo.getRGB(calcWidth, calcHeight))

          // Get all colors values
          val red = pixelColor.getRed
          val green = pixelColor.getGreen
          val blue = pixelColor.getBlue

          // Relative luminance formula
          val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
          // Sum accumulator with calculated value from formula and go to the next pixel in column
          helper(accumulator + luminance, calcWidth, calcHeight + 1)
        }
    }
    helper(0, 0, 0)
  }
  // Function to calculate value for all white image of the original size
  def calculateMaxWhiteValue(photo: BufferedImage): Double = {
    val whiteColorVal = 255 // Value of a white pixel (255, 255, 255) in RGB
    photo.getWidth * photo.getHeight * (0.2126 * whiteColorVal + 0.7152 * whiteColorVal + 0.0722 * whiteColorVal)
  }
  // Function to prepare name of the image to save
  def prepareName(workerName: String, threshold: Int, result: Double, outPath: String): (String, String) = {

    // Name of the original file without extension
    val nameWithoutExtension = workerName.split('.').head

    // Format of the original file "jpg" or "png"
    val format = workerName.split('.').last

    // Prepare dark or bright metadata
    val resultString = if (result * 100 > threshold) "dark" else "bright"

    // Convert from double to int 0.57 -> 57
    val formatResult = (result * 100).toString.split('.').head

    // Return tuple containing (path, format)
    (outPath + "/" + nameWithoutExtension + "_" + resultString + "_" + formatResult + "." + format, format)
  }
}

object Main extends App {

  // Initialize akka actors system
  val system = ActorSystem("system")

  // Create boss actor (head of the system)
  val boss = system.actorOf(Props[Boss], name="boss")

  // Settings
  val THRESHOLD = 75 // Value used to define the bright/dark barrier
  val outDir = "src/main/out" // Directory to save checked pictures
  val sourceDir = "src/main/in" // Directory from where the program gets samples

  // Apply settings from above to boss actor
  boss ! Settings(THRESHOLD, outDir)

  // Get input folder as a File
  val inputFolder = new File(sourceDir)

  // Get files list in input folder
  val files = inputFolder.listFiles().toList

  // Send every file to boss
  files.map(file => boss ! Path(file.getPath))
}
