package com.uplpipe

import java.io.{File, FileOutputStream, FileInputStream, BufferedOutputStream}

object Config {
	def chunk     = 8*1024
	def basePath  = "/tmp/pqp"
	def fieldName = "f"
}
class Session {
	def update(uuid :String, readed :Int, max :Int) = {}
	def setComplete(uuid:String) = {}
}
class UplpipeWebApp(session :Session) {
	def handleUpload(uuid :String, contentLength :Int, contentType :String, inputReadLine: (Array[Byte], Int, Int) => Int) = {
		val input     = new UplpipeServletInputStream(contentLength, inputReadLine, { 
			(readed :Int) => session.update(uuid,readed,contentLength) 
		})
		
		val boundary  = extractBoundary(contentType)
		val parser    = new UplpipeMultipartRequestParser(uuid, boundary, input)
		
		val file      = parser.getFileFromInput(Config.fieldName).getOrElse{ errorFileNotFound }
		
		val changed = _rename(file)
		session.setComplete(uuid)
	}
	
	val contentRegex = "multipart/form-data; boundary=(-+\\w+)".r

	def errorBoundary = throw new Exception("it is not a multipart request!")

	def extractBoundary(content:String):String = 
		contentRegex findFirstMatchIn(content) map { "--" + _.group(1) } getOrElse { errorBoundary } 

	def errorFileNotFound = throw new Exception("file not found!")
	
	def _rename(tmpfile :File) :Boolean = tmpfile.renameTo(new File(tmpfile.getPath.replace("incoming-","")))	
}

class UplpipeMultipartRequestParser(uuid :String, boundary :String, input :UplpipeServletInputStream){
	val dispositionRegex  = """.*Content-Disposition: form-data; name="(.+)"; filename="(.+)".*""".r
	
	if (! input.readLine.startsWith(boundary) ) throw new IllegalArgumentException("data corrupt")
	
	def getFileFromInput(name :String):Option[File] = {
		val contentDisposition = input readLine
		var contentType        = input readLine
		var emptyLine          = input readLine
		
		if(contentType.isEmpty || !emptyLine.isEmpty) error_malformed_content_disposition

		val fieldName  = dispositionRegex findFirstMatchIn(contentDisposition) map { _.group(1) } getOrElse { 
			error_malformed_content_disposition
		}

		if (fieldName != name) return None 
		
		val file  = new File("%s/incoming-%s".format(Config.basePath,uuid))
		val fos   = new FileOutputStream(file)
		val out   = new BufferedOutputStream(fos, 8*1024)
		val bbuf  = new Array[Byte](8*1024)
		
		var result = -1
		while({ result = input.readLine(bbuf, 0, bbuf.length) 
				result != -1 && ! reach_end_of_part(bbuf,result) }){
				out.write(bbuf, 0, result);
		}
		
		out.flush ; out.close ; fos.close
		
		_truncate(file)
		
		Option(file)
	}

	def reach_end_of_part(bbuf :Array[Byte], result:Int) : Boolean = 
		result > 2 && bbuf(0) == '-' && bbuf(1) == '-' && new String(bbuf, 0, result).startsWith(boundary)
		
	def error_malformed_content_disposition = throw new IllegalArgumentException("malformed Content-Disposition")		

	def _truncate(file :File) = {
		val fos  = new FileOutputStream(file,true)
		val chan = fos.getChannel() // truncate by last 2 characteres
		chan.truncate(chan.size -2)
		chan.close() ; fos.close()
	}
}

class UplpipeServletInputStream( 
	maxBytes :Int,
	inputReadLine: (Array[Byte], Int, Int) => Int, 
	onUpdate :Int => Unit,
	chunk :Int = Config.chunk) {
	
	var readedBytes = 0 
	
	def readLine:String = {
		var result = 0
		val buf = new Array[Byte](chunk)
		val sbuf = new StringBuffer()
		
		do {
			result = readLine(buf, 0, buf.length)
			if (result != -1) sbuf.append(new String(buf, 0, result))
		}while (result == buf.length)
		
	    sbuf.toString.stripLineEnd
	}
	
 	def readLine(b :Array[Byte], off:Int, len:Int): Int = 
		if (maxBytes > readedBytes) {
			var result = inputReadLine(b, off, len)
			readedBytes += result 
			onUpdate(readedBytes)
			
			result
	    } else {
			-1 // update complete!!!
		}	
}