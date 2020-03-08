import java.io.{File, FileOutputStream}
import java.nio.channels.Channels

import scala.io._
import scala.xml.{Elem, PrettyPrinter, XML}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

// get list of files in path
def getListOfFiles(dir: String): List[String] = {
  val file = new File(dir)
  file.listFiles.filter(_.isFile)
    .map(_.getPath)
    .filter(_.endsWith(".json"))
    .toList
}

// parse json file
def parseJson(filePath: String): Map[String, Object] = {
  val json = Source.fromFile(filePath)
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  mapper.readValue[Map[String, Object]](json.reader())
}

def getShapes(jsonInfo: Map[String, Object]) = {
  try {
    val shapes = jsonInfo("shapes").asInstanceOf[List[Map[String, Object]]]
    Some(shapes)
  } catch {
    case e: Exception =>
    println(e)
    None
  }
}

def getImgPath(jsonInfo: Map[String, Object]): String = {
  val json = jsonInfo("imagePath").asInstanceOf[String]
  json
}

// convert shape to xml tags
def shapeToXml(m: Option[List[Map[String, Object]]]) = {
  try {
    val shape = m.get
    Some(shape.map(createXmlWithShape))
  } catch {
    case e: Exception =>
      println(e)
    None
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
          <width>644</width>
          <height>482</height>
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

val dirPath = "/Users/kjsong/Downloads/log_imgs_devel2_inje"
val jsonFilePaths = getListOfFiles(dirPath+"/Annotations")
val shapes = jsonFilePaths
  .map(parseJson)
  .map(getShapes)
  .map(shapeToXml)

saveXml(dirPath, shapes, jsonFilePaths)