import java.io.{File, FileOutputStream}
import java.nio.channels.Channels

import play.api.libs.json.{JsObject, Json}


// get list of files in path
def getListOfFiles(dir: String, extension: String): List[String] = {
  val file = new File(dir)
  file.listFiles.filter(_.isFile)
    .map(_.getPath)
    .filter(_.endsWith(extension))
    .toList
}

def createDefaultJson(filename: String) = {
  Json.obj(
    "imageHeight" -> 644,
    "imageWidth" -> 482,
    "imagePath" -> filename
  )
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

val defaultPath = "/Users/kjsong/Downloads/log_imgs_devel2_inje"
val jpgPath = defaultPath + "/JPEGImages"
val jsonPath = defaultPath + "/Annotations"

val jpgFiles = getListOfFiles(jpgPath, ".jpg")
  .map(_.split("/").last)
  .map(x => x.splitAt(x.lastIndexOf("."))._1)

val jsonFiles = getListOfFiles(jsonPath, ".json")
  .map(_.split("/").last)
  .map(x => x.splitAt(x.lastIndexOf("."))._1)

val omittedJsonFiles = (jpgFiles diff jsonFiles)
  .map(_+".json")
val omittedJsonData = omittedJsonFiles
  .map(createDefaultJson)

saveJson(jsonPath, omittedJsonFiles, omittedJsonData)