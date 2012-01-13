package com.uplpipe

import java.io.{File, FileOutputStream, FileInputStream, BufferedOutputStream}
import javax.servlet.ServletInputStream
//import javax.servlet.annotation.{ MultipartConfig, WebServlet }
import javax.servlet.http.{ HttpServlet, HttpServletRequest => HSReq, HttpServletResponse => HSResp }
import scala.collection.mutable.HashMap
import org.apache.log4j.{BasicConfigurator, Logger, FileAppender, PatternLayout}

object Config {
	def chunk      = 8*1024
	def basePath   = "/tmp"
	def fieldName  = "f"
	def bucketname = "uplpipe"
}

object session {
	val db = new HashMap[String,(Double,String)]
	def setComplete( uuid :String)                    = db(uuid)  = (     100.0, "completed"  )
	def hasNot( uuid :String):Boolean                 = ! db.isDefinedAt(uuid)
	def setError( uuid :String )                      = db(uuid)  = (       0.0, "error"      )
	def setProgress(uuid :String, percentage :Double) =	db(uuid)  = (percentage, "in progress")
	def get( uuid: String ): (Double,String)          = db getOrElse(uuid, (0.0, "not started"))
}

//@WebServlet(name = "UploadServlet", urlPatterns = Array("/upload/*"))
//@MultipartConfig(maxFileSize=1024*1024*700, maxRequestSize=1024*1024*700)
class UploadServlet extends HttpServlet {
	val webapp  = new UplpipeWebApp
	val patternUrl = ".*/upload/([a-fA-F0-9]{12}-[^/]*)(?:/(\\w+))?$".r
	
	override def doGet(req : HSReq, resp : HSResp) = req.getRequestURI() match { 
			case patternUrl(uuid,"status")  =>
				val json = webapp.status(uuid, req.getRequestURI)
				resp.setContentType("application/json")
				resp.getWriter.print(json)
			
			case patternUrl(uuid,"download")  =>
				val file = new File(Config.basePath + "/" + uuid)
				if (file.exists) {
					transferFileTo(file,resp)
				} else {
					resp setContentType("application/x-download")
					resp sendRedirect "http://s3.amazonaws.com/%s/%s".format(Config.bucketname,uuid)
				}
			
			case _ => resp.sendError(404,"not found")	
		}
	override def doPost(req : HSReq, resp : HSResp) = req.getRequestURI() match { 
			case patternUrl(uuid,null)      => 
				webapp.handleUpload(uuid,req.getContentLength, req.getContentType, req.getInputStream)
				resp.getWriter.print("ok")
				
			case patternUrl(uuid,"message") =>
			    req.setCharacterEncoding("UTF-8") 
				val html = webapp.message(req.getParameter("message"),req.getRequestURI)
				resp.setContentType("text/html; charset=UTF-8")
				resp.getWriter.print(html)
				
			case _                          => 
				resp.sendError(404,"not found")
		}
		
	def transferFileTo(file :File,resp :HSResp) = {
		val out   = resp.getOutputStream
		val is    = new FileInputStream(file)
		val bytes = new Array[Byte](8 * 1024)
		var read  = is.read(bytes)

		resp setContentType("application/x-download")
		resp setHeader("Content-Disposition", "attachment; filename=" + file.getName);
		resp setHeader("Content-Length",file.length.toString)

		while(read != -1){
			out write(bytes,0, read)
			read = is.read(bytes)
		}

		out.flush; out.close
	}		
}
class UplpipeWebApp {
	BasicConfigurator.configure(new FileAppender(new PatternLayout(),"/tmp/a.log",true))
	val logger = Logger.getLogger(classOf[UplpipeWebApp])
	
	def message(msg :String, uri :String) = {
		val url = uri.replace("/message","/download")
		
		<html>
			<head>
				<title>uplpipe</title>
			</head>
			<body>
				<p>File: click <a href={url}>here</a> to download</p>
				<p>Message:</p>
				<p>{msg}</p>
				<p>please send another file <a href="/">here</a></p>
			</body>
		</html>
	}
	
	def status(uuid :String, uri :String): String = {
		val (percentage,status) = session.get(uuid)
		val url = uri.replace("/status","/download")
		val jsonTemplate = """{ "percentage" : %.02f, "status" : "%s" }"""
		
		jsonTemplate format(percentage,status)
	}
	
	def handleUpload(uuid :String, contentLength :Int, contentType :String, inputStream :ServletInputStream)  = {
		try{
			logger.debug("start handleUpload for uuid="+uuid)
			val boundary  = extractBoundary(contentType)
		
			val input     = new UplpipeServletInputStream(contentLength, inputStream, onUpdate={ 
				(readed :Int) => session.setProgress(uuid, 100.0* readed.toDouble / contentLength.toDouble) 
			})
		
			val parser    = new UplpipeMultipartRequestParser(uuid, boundary, input)
		
			val file      = parser.getFileFromInput(Config.fieldName).getOrElse{ errorFileNotFound }
					
			session.setComplete(uuid)
			logger.debug("end handleUpload for uuid=%s, size=%d\n".format(uuid,file.length))
		} catch{
			case e:Exception => 
				session.setError(uuid); 
				logger.error("error @ handleUpload for uuid=" + uuid,e)
				throw e
		}
	}
	
	val contentRegex = "multipart/form-data; boundary=(-+\\w+)".r

	def errorBoundary = throw new IllegalArgumentException("it is not a multipart request!")

	def extractBoundary(content:String):String = 
		contentRegex findFirstMatchIn(content) map { "--" + _.group(1) } getOrElse { errorBoundary } 

	def errorFileNotFound = throw new Exception("file not found!")	
}

class UplpipeMultipartRequestParser(uuid :String, boundary :String, input :UplpipeServletInputStream){ 
	if (! input.readLine.startsWith(boundary)) throw new IllegalArgumentException("data corrupt")

	val dispositionRegex  = """.*Content-Disposition: form-data; name="(.+)"; filename="(.+)".*""".r
	
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
		if(!util.truncate(file)) throw new Exception("cannot truncate file")
	
		val (renamed, nfile) = util.rename(file)
		
		if(renamed) Option(nfile) else throw new Exception("cannot rename file")
	}
	
	def reach_end_of_part(bbuf :Array[Byte], result:Int) : Boolean = 
		result > 2 && bbuf(0) == '-' && bbuf(1) == '-' && new String(bbuf, 0, result).startsWith(boundary)
		
	def error_malformed_content_disposition = throw new IllegalArgumentException("malformed Content-Disposition")		
}

object util{
	def rename(tmpfile :File) :(Boolean,File) = {
		val file = new File(tmpfile.getPath.replace("incoming-",""))
		
		(tmpfile.renameTo(file),file)
	}

	def truncate(file :File) = {
		val old  = file.length
		val fos  = new FileOutputStream(file,true)
		val chan = fos.getChannel() // truncate by last 2 characteres
		chan.truncate(chan.size -2)
		chan.close() ; fos.close()
		
		old - file.length == 2
	}
}

class UplpipeServletInputStream( 
	maxBytes :Int,
	input :ServletInputStream, 
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
			var result = input.readLine(b, off, len)
			readedBytes += result 
			onUpdate(readedBytes)
			
			result
	    } else {
			-1 // update complete!!!
		}	
}
