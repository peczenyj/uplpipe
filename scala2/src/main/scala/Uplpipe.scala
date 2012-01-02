package com.uplpipe

import java.io.File
object Config {
	def chunk = 8*1024
}
class UplpipeMultipartRequestParser(uuid :String, boundary :String, input :UplpipeServletInputStream){
	if (! input.readLineString.startsWith(boundary) ) throw new IllegalArgumentException("data corrupt")
	
	def getFileFromInput(name :String):Option[File] = {
		val contentDisposition = input readLineString
		var contentType        = input readLineString
		var emptyLine          = input readLineString
		
		if(contentType.isEmpty || !emptyLine.isEmpty) error_malformed_content_disposition

		val fieldName  = extractFieldName(contentDisposition)
		val fileName   = extractFilename(contentDisposition)	
		
		if (fieldName != name) return None 
		
		Option(new File("/tmp/a"))
	}
	val dispositionRegex  = """Content-Disposition: form-data; name="(.+)"; filename="(.+)"""".r	

	def error_malformed_content_disposition = throw new IllegalArgumentException("malformed Content-Disposition")

	def extractFieldName(line:String):String = 
		dispositionRegex findFirstMatchIn(line) map { _.group(1) } getOrElse { error_malformed_content_disposition } 

	def extractFilename(line:String): String = 
		dispositionRegex findFirstMatchIn(line) map { _.group(2) } getOrElse { error_malformed_content_disposition }		
}

class UplpipeServletInputStream( 
	maxBytes :Int,
	inputReadLine: (Array[Byte], Int, Int) => Int, 
	onUpdate :Int => Unit,
	chunk :Int = Config.chunk) {
	
	var readedBytes = 0 
	
	def readLineString:String = {
		var result = 0
		val buf = new Array[Byte](chunk)
		val sbuf = new StringBuffer()
		
		do {
			result = readLineArray(buf, 0, buf.length)
			if (result != -1) sbuf.append(new String(buf, 0, result))
		}while (result == buf.length)
		
	    sbuf.toString.stripLineEnd
	}
	
 	def readLineArray(b :Array[Byte], off:Int, len:Int): Int = 
		if (maxBytes > readedBytes) {
			var result = inputReadLine(b, off, len)
			readedBytes += result 
			onUpdate(readedBytes)
			
			result
	    } else {
			-1 // update complete!!!
		}	
}