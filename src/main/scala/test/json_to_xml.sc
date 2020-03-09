import java.io.{File, FileOutputStream}
import java.nio.channels.Channels

import scala.io._
import scala.xml.{Elem, PrettyPrinter, XML}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import javax.imageio.ImageIO

// get list of files in path
def getListOfFiles(dir: String): List[String] = {
  val file = new File(dir)
  file.listFiles.filter(_.isFile)
    .map(_.getPath)
    .filter(_.endsWith(".json"))
    .toList
}

// parse json file
def parseJson(filePath: String) = {
  val json = Source.fromFile(filePath)
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  Map("filePath" -> filePath,
  "json" -> mapper.readValue[Map[String, Object]](json.reader()))
}

def getShapes(data: Map[String, Object]) = {
  val filePath = data("filePath").asInstanceOf[String]
  val json = data("json").asInstanceOf[Map[String, Object]]
  val shapes = json.get("shapes")
  if(shapes.isDefined) {
    Map(
      "filePath" -> filePath,
      "shapes" -> shapes.get.asInstanceOf[List[Map[String, Object]]]
    )
  } else {
    Map(
      "filePath" -> filePath)
  }
}

// convert shape to xml tags
def shapeToXml(m: Map[String, Object]) = {
  val filePath = m("filePath").asInstanceOf[String]
  val shape = m.get("shapes")
  if(shape.isDefined) {
    Map(
      "filePath" -> filePath,
      "shapeXml" -> shape.get.asInstanceOf[List[Map[String, Object]]]
        .map(createXmlWithShape)
    )
  } else {
    Map(
      "filePath" -> filePath
    )
  }
}

def createXmlWithShape(m: Map[String, Object]): scala.xml.Elem = {
  val name = m("label").asInstanceOf[String].toLowerCase()
  val points = m("points").asInstanceOf[List[List[AnyVal]]]

  <object>
    <name>{name}</name>
    <pose>Unspecified</pose>
    <truncated>1</truncated>
    <difficult>0</difficult>
    <bndbox>
      <xmin>{points(0)(0)}</xmin>
      <ymin>{points(0)(1)}</ymin>
      <xmax>{points(1)(0)}</xmax>
      <ymax>{points(1)(1)}</ymax>
    </bndbox>
  </object>
}

// append new xml to original xml
def appendXml(originalXML: Elem, xml: Elem) = {
  originalXML match {
    case <annotation>{ innerProps @ _* }</annotation> => {
      <annotation> {
        innerProps ++ xml
      }</annotation>
    }
    case other => other
  }
}

def createXML(filename: String, path: String, shapes: Option[List[Elem]]) = {
  val img = ImageIO.read(new File(path))
  val imgWidth = img.getWidth()
  val imgHeight = img.getHeight()

  <annotation>
    <folder>
      JPEGImages
    </folder>
    <filename>
      {filename}
    </filename>
    <path>
      {path}
    </path>
    <source>
      <database>Unknown</database>
    </source>
    <size>
      <width>{imgWidth}</width>
      <height>{imgHeight}</height>
      <depth>3</depth>
    </size>
    <segmented>0</segmented>
    {if(shapes.isDefined) shapes.get}
  </annotation>
}

def saveXml(dirPath: String, shapes: List[Option[List[Elem]]], jsonFilePaths: List[String]) = {
  val printer = new PrettyPrinter(80, 2)

  val m = (jsonFilePaths zip shapes).toMap
  for((jsonFilePath, shape) <- m) {
    val fileHeader = jsonFilePath.split("/").last.dropRight(5)
    val xmlFileFullPath = dirPath+"/Annotations/"+fileHeader+".xml"
    val jpgFileName = fileHeader+".jpg"
    val jpgFileFullPath = dirPath+"/JPEGImages/"+jpgFileName
    val fos = new FileOutputStream(xmlFileFullPath)
    val writer = Channels.newWriter(fos.getChannel(), "UTF-8")
    try {
      writer.write(printer.format(createXML(jpgFileName, jpgFileFullPath, shape)))
    } finally {
      writer.close()
    }
  }
}

def save(data: Map[String, Object]) = {
  val filePath = data("filePath").asInstanceOf[String]
  val dirPath = filePath.splitAt(
    filePath.lastIndexOf("/Annotations"))
    ._1
  val filenamePrefix = filePath.split("/")
    .last
    .split("\\.json")
    .head
  val xmlFileFullPath = dirPath+"/Annotations/"+filenamePrefix+".xml"
  val jpgFileName = filenamePrefix+".jpg"
  val jpgFileFullPath = dirPath+"/JPEGImages/"+jpgFileName

  val fos = new FileOutputStream(xmlFileFullPath)
  val writer = Channels.newWriter(fos.getChannel, "UTF-8")
  try {
    val printer = new PrettyPrinter(80, 2)
    writer.write(printer.format(createXML(jpgFileName, jpgFileFullPath,
      data.get("shapeXml") match {
        case Some(x) => Some(x.asInstanceOf[List[Elem]])
        case None => None
      })))
  } finally {
    writer.close()
  }
}

val dirPath = "/Users/kjsong/Downloads/log_imgs_devel2_inje"
val jsonFilePaths = getListOfFiles(dirPath+"/Annotations")
jsonFilePaths
  .map(parseJson)
  .map(getShapes)
  .map(shapeToXml)
  .map(save)

//saveXml(dirPath, shapes, jsonFilePaths)