import java.net.URL
import java.util.UUID
import java.io.{File, FileOutputStream, FileInputStream, BufferedOutputStream}
import javax.servlet.{ServletInputStream, ServletContext}
import javax.servlet.annotation.{ MultipartConfig, WebServlet }
import javax.servlet.http.{ HttpServlet, HttpServletRequest => HSReq, HttpServletResponse => HSResp }
import scala.collection.mutable.HashMap

@WebServlet(name = "UploadServlet", urlPatterns = Array("/upload/*"))
@MultipartConfig(maxFileSize=1024*1024*700, maxRequestSize=1024*1024*700)
class TestServlet extends HttpServlet {
	val patternUrl = ".*/upload/([a-fA-F0-9]{12}-[^/]*)(?:/(\\w+))?$".r
		
	override def doPost(req : HSReq, resp : HSResp) = req.getRequestURI() match { 
			case patternUrl(uuid,null)      => UploadController create(uuid,req,resp) 
			case patternUrl(uuid,"message") => UploadController message(uuid,req,resp)
			case _                          => UploadController sendError(404,resp)
	}
	
	override def doGet(req : HSReq, resp : HSResp) = req.getRequestURI() match { 			
			case patternUrl(uuid,"status")   => UploadController status(uuid,req,resp)
			case patternUrl(uuid,"download") => UploadController download(uuid,req,resp)
			case _                           => UploadController sendError(404,resp)
	}
}
object UploadController {
	def sendError(code :Int, resp: HSResp) = resp sendError code
	
	def create(uuid : String, req : HSReq, resp : HSResp) = 
		if(session hasNot uuid) handleUpload(uuid,req,resp) else sendError(403,resp)
	
	def message(uuid : String, req : HSReq, resp : HSResp)  = {
		val txt = req getParameter("message")
		val url = req getParameter("download")
		req.getSession.getServletContext.log("txt=> '%s'".format(txt))
		
		//resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter.print(messageTemplate(txt,url))
	}
	
	def status(uuid : String, req : HSReq, resp : HSResp)   = {
		val (percentage,status,_) = session.get(uuid)
		val url = req.getRequestURI.replace("/status","/download")
		val jsonTemplate = """{ "percentage" : %.02f, "status" : "%s",  "link" : "%s" }"""
		val json = jsonTemplate format(percentage,status,url)
		
		resp setContentType("application/json")
		resp.getWriter.print(json)
	}
	
	def download(uuid : String, req : HSReq, resp : HSResp) = {
		val (_,status,url) = session get(uuid)
		if(status != "completed") sendError(404,resp) else
		url.getProtocol match {
			case "file" => transferFileTo(url,resp)
			case _      => resp sendRedirect url.toString   
		}
	}
	
	def messageTemplate(txt:String,url:String) =
		<html>
			<head>
				<title>uplpipe</title>
			</head>
			<body>
				<p>File: click <a href={url}>here</a> to download</p>
				<p>Message:</p>
				<textarea cols="60" rows="5">{txt}</textarea>
			</body>
		</html>

	def	handleUpload(uuid : String, req : HSReq, resp : HSResp) = {
		try{ 
			val inputStream   = req getInputStream()
			val contentLength = req getContentLength()
			val boundary      = extractBoundary(req getContentType())
			var input         = new MultipartReader(inputStream,contentLength, uuid)
			val reader        = new MultipartParser(input, boundary)
			val url           = reader.toMap.getOrElse("f", errorFileNotFound)
			session.setComplete(uuid,url)
		} catch { 
			case e => session.setError(uuid) 
			throw e 
		}
	}

	val contentRegex = "multipart/form-data; boundary=(-+\\w+)".r

	def errorBoundary = throw new Exception("it is not a multipart request!")

	def extractBoundary(content:String):String = 
		contentRegex findFirstMatchIn(content) map { "--" + _.group(1) } getOrElse { errorBoundary } 

	def errorFileNotFound = throw new Exception("file not found!")
	
	def transferFileTo(url:URL,resp : HSResp) = {
		val file = new File(url.getFile())
		
		val out = resp.getOutputStream
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
object Config {
	val basePath = "/tmp/scala"
}
object session {
	val db = new HashMap[String,(Double,String,URL)]
	def hasNot( uuid :String):Boolean                 = ! db.isDefinedAt(uuid)
	def setError( uuid :String )                      = db(uuid)  = (       0.0, "error"      ,null)
	def setComplete( uuid :String, file :URL)         = db(uuid)  = (     100.0, "completed"   ,file)
	def setProgress(uuid :String, percentage :Double) =	db(uuid)  = (percentage, "in progress",null)
	def get( uuid: String ): (Double,String,URL)      = db getOrElse(uuid, (0.0, "not started", null))
}
object util {	
	def truncate(file :File) = {
		val fos  = new FileOutputStream(file,true)
		val chan = fos.getChannel() // truncate by last 2 characteres
		chan.truncate(chan.size -2)
		chan.close() ; fos.close()
	}
}
class MultipartParser(input :MultipartReader, boundary :String) extends Iterator[(String,URL)] {
	var lastContentDisposition = ""
	
	if (! input.readLine.startsWith(boundary) ) throw new Exception("data corrupt")

	def hasNext():Boolean = {
		lastContentDisposition = input readLine
		var contentType        = input readLine
		var emptyLine          = input readLine
		
		!lastContentDisposition.isEmpty && !contentType.isEmpty && emptyLine.isEmpty
	}
		
	def next: (String, URL) = {
		val fieldName  = extractFieldName(lastContentDisposition)
		val fileName   = extractFilename(lastContentDisposition)
		
		val file  = new File("%s/%s-%s".format(Config.basePath,UUID.randomUUID().toString(),fileName))
		val fos   = new FileOutputStream(file)
		val out   = new BufferedOutputStream(fos, 8*1024)
		val bbuf  = new Array[Byte](8*1024)
		
		var result = -1
		while({ result = input.readLine(bbuf, 0, bbuf.length) 
				result != -1 && ! reach_end_of_part(bbuf,result) }){
			out.write(bbuf, 0, result);
		}
		
		out.flush ; out.close ; fos.close
		
		util.truncate(file)
		
		(fieldName -> new URL("file","",file.getPath))
	}

	def reach_end_of_part(bbuf :Array[Byte], result:Int) : Boolean = 
		result > 2 && bbuf(0) == '-' && bbuf(1) == '-' && new String(bbuf, 0, result).startsWith(boundary)

	val dispositionRegex  = """Content-Disposition: form-data; name="(.+)"; filename="(.+)"""".r	

	def error = throw new Exception("malformed Content-Disposition")

	def extractFieldName(line:String):String = 
		dispositionRegex findFirstMatchIn(line) map { _.group(1) } getOrElse { error } 

	def extractFilename(line:String): String = 
		dispositionRegex findFirstMatchIn(line) map { _.group(2) } getOrElse { error } 
}
class MultipartReader(in :ServletInputStream, maxBytes :Int, uuid :String) {
	var readedBytes = 0
	val buf = new Array[Byte](8*1024) 
	
	def readLine():String = {
		var result = 0
		val sbuf = new StringBuffer()
		do {
			result = readLine(buf, 0, buf.length)
			if (result != -1) sbuf.append(new String(buf, 0, result))
		}while (result == buf.length)
		
	    sbuf.toString.stripLineEnd
	}
	
 	def readLine(b :Array[Byte], off:Int, len:Int): Int = 
		if (maxBytes > readedBytes) {
			var result = in.readLine(b, off, len)
			readedBytes += result 
			session.setProgress(uuid,100.0 *readedBytes.toDouble/maxBytes.toDouble)
			result
	    } else {
			-1 // update complete!!!
		}
}

@WebServlet(name = "StatsServlet", urlPatterns = Array("/stats"))
class StatsServlet extends HttpServlet {
	override def doGet(req : HSReq, resp : HSResp) = {
		val table = 
		<table>
			<tr><th>uuid</th><th>progress</th><th>status</th><th>link</th></tr>
			{for ((uuid, (progress,status,link)) <- session.db) 
					yield <tr><td>{uuid}</td><td>{progress}</td><td>{status}</td><td>{link}</td></tr>
			}
		</table>
		
		resp.getWriter.print(table)
	}
}