import java.io.FileOutputStream
import java.nio.channels.Channels

import scala.io._
import scala.xml.{Elem, PrettyPrinter, XML}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

// Create Json parser
val json = Source.fromFile("/Users/kjsong/Desktop/55651.json")
val mapper = new ObjectMapper() with ScalaObjectMapper
mapper.registerModule(DefaultScalaModule)
val parsedJson = mapper.readValue[Map[String, Object]](json.reader())

// Create basic tags in xml
var originalXML =
  <annotation>
    <folder>JPEGImages</folder>
  </annotation>

// Get shapes in json
val shapes = parsedJson.get("shapes").toList.head.asInstanceOf[List[Map[String, Object]]]

// convert shapes to xml tags
// update originalXML
for(xml <- shapes.map(shapeToXml)) {
  originalXML = appendXml(originalXML, xml)
}

// convert shape to xml tags
def shapeToXml(m: Map[String, Object]): scala.xml.Elem = {
  val name = m("label").asInstanceOf[String].toLowerCase()
  val points = m("points").asInstanceOf[List[List[AnyVal]]]

  <object>
    <name>{name}</name>
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

// write xml to file
val fileName = "/Users/kjsong/Desktop/result.xml"
val fos = new FileOutputStream(fileName)
val writer = Channels.newWriter(fos.getChannel(), "UTF-8")
val printer = new PrettyPrinter(80, 2)
try {
  writer.write(printer.format(originalXML))
} finally {
  writer.close()
}
