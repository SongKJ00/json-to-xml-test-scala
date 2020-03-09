import java.io.{File, FileOutputStream}
import java.nio.channels.Channels

import javax.imageio.ImageIO
import play.api.libs.json.{JsObject, JsValue, Json}


// get list of files in path
def getListOfFiles(dir: String, extension: String): List[String] = {
  val file = new File(dir)
  file.listFiles.filter(_.isFile)
    .map(_.getPath)
    .filter(_.endsWith(extension))
    .toList
}

def createDefaultJson(filename: String, jpgDir: String) = {

  val jpgFileName = jpgDir + "/" +
    filename.split("/").last.replace("\\.json", "\\.jpg")
  val img = ImageIO.read(new File(jpgFileName))
  val imgWidth = img.getWidth()
  val imgHeight = img.getHeight()

  Map(
    "filename" -> filename,
    "json" -> Json.obj(
      "imageHeight" -> imgHeight,
      "imageWidth" -> imgWidth,
      "imagePath" -> filename
  ))
}

def saveJson(path: String, filenames: List[String], jsonDataList: List[JsObject]) = {
  val m = (filenames zip jsonDataList).toMap
  for((filename, jsonData) <- m) {
    val fos = new FileOutputStream(path+"/"+filename)
    val writer = Channels.newWriter(fos.getChannel, "UTF-8")
    try {
      writer.write(Json.prettyPrint(jsonData))
    } finally {
      writer.close()
    }
  }
}

def save(data: Map[String, Any]) = {
  val filePath = data("filename").asInstanceOf[String]
  val json = data("json").asInstanceOf[JsValue]
  val fos = new FileOutputStream(filePath)
  val writer = Channels.newWriter(fos.getChannel, "UTF-8")
  try {
    writer.write(Json.prettyPrint(json))
  } finally {
    writer.close()
  }
}

val defaultPath = "/Users/kjsong/Downloads/log_imgs_devel2_inje"
val jpgPath = defaultPath + "/JPEGImages"
val jsonPath = defaultPath + "/Annotations"

val jpgFilePrefixes = getListOfFiles(jpgPath, ".jpg")
  .map(_.split("/").last)
  .map(x => x.splitAt(x.lastIndexOf("."))._1)

val jsonFilePrefixes = getListOfFiles(jsonPath, ".json")
  .map(_.split("/").last)
  .map(x => x.splitAt(x.lastIndexOf("."))._1)

val omittedJsonFiles = (jpgFilePrefixes diff jsonFilePrefixes)
  .map(jsonPath + "/" + _ + ".json")
  .map(createDefaultJson(_, jpgPath))
  .map(save)

//saveJson(jsonPath, omittedJsonFiles, omittedJsonData)