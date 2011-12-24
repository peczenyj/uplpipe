import javax.servlet.http.{
	HttpServlet,
  	HttpServletRequest => HSReq, 
	HttpServletResponse => HSResp
}

import javax.servlet.annotation.{
	MultipartConfig,
	WebServlet
}

import scala.collection.mutable.HashMap

@WebServlet(name = "UploadServlet", urlPatterns = Array("/upload/*"))
@MultipartConfig(maxFileSize=1024*1024*50, maxRequestSize=1024*1024*50)
class TestServlet extends HttpServlet {
	val patternUrl = "/upload/([a-fA-F0-9]{12}-[^/]*)(?:/(\\w+))?$"
	
	override def doPost(req : HSReq, resp : HSResp) = 
	{
		req.getRequestURI() match { 
			case patternUrl(uuid,null)   =>
				UploadService.create(uuid,req,resp) 
			case patternUrl(uuid,"message") => 
				UploadService.message(uuid,req,resp)
				
			case _ => send404(resp)
		}
	}
	
	override def doGet(req : HSReq, resp : HSResp) =
	{
		req.getRequestURI() match { 
			case patternUrl(uuid,"status") => 
				UploadService.status(uuid,req,resp)
			case patternUrl(uuid,"download") => 
				UploadService.download(uuid,req,resp)
				
			case _ => send404(resp)
		}
	}	
			
	def send404(resp : HSResp) = {
		resp.sendError(resp.SC_NOT_FOUND)
	}
}

object UploadService {
	def getUploadFromUUID(uuid : String) = UploadRepository.get(uuid)
	def messageTemplate(u : Upload) = 
    <HTML>
      <HEAD><TITLE>Upload Pipe</TITLE></HEAD>
      <BODY>
	    <p>file : <a href="{u.link}">download</a></p>
	    <p>message : {u.message} </p>	
	  </BODY>
    </HTML>

	def create(uuid : String, req : HSReq, resp : HSResp) = {
		val upload = getUploadFromUUID(uuid)
		
	}
	def message(uuid : String, req : HSReq, resp : HSResp)  = {
		val upload = getUploadFromUUID(uuid)
		upload.message(req.getParameter("message"))
		resp.getWriter().print(messageTemplate(upload))
	}
	def status(uuid : String, req : HSReq, resp : HSResp)   = {
		val upload = getUploadFromUUID(uuid)
		resp.setContentType("application/json")
		resp.getWriter().print(upload status)
	}
	def download(uuid : String, req : HSReq, resp : HSResp) = {
		val upload = getUploadFromUUID(uuid)
		resp.sendRedirect(upload.link)
	}
}

object UploadRepository {
	def get(uuid: String) : Upload = {
		dao get(uuid, null)
	}
	def store(u) = {
		dao update(u.uuid,u)
	}
}

object dao {
	val database = new HashMap[String,Upload]
}

class Upload {
	def uuid   = "aabbccdd1234"
	def link   = "google.com"
	def status = """{  }"""
}