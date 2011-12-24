import javax.servlet.http.{HttpServlet,
  	HttpServletRequest => HSReq, 
	HttpServletResponse => HSResp
}
import java.io.{
	FileOutputStream,
	FileInputStream
}
import javax.servlet.annotation.{
	MultipartConfig,
	WebServlet
}

@WebServlet(name = "UploadServlet", urlPatterns = Array("/upload/*"))
@MultipartConfig(maxFileSize=1024*1024*50, maxRequestSize=1024*1024*50)
class TestServlet extends HttpServlet {
	
	override def doPost(req : HSReq, resp : HSResp) = 
	{
		resp.getWriter().print(
			scala.io.Source.fromInputStream(
				req.getInputStream()
			).getLines().mkString("\r\n")
		)
	}
	
	override def doGet(req : HSReq, resp : HSResp) =
	{
		resp.setHeader("Content-Type","application/json")
		resp.getWriter().print("{ oi : 1}")	
	}	
}

object UploadController {
	var p = 0l;
	var max = 1l;
	
	def clean = { p = 0l; max=1l;}
	def value = 100.0 * p/max
	def incr(n : Long) = { p = p + n }
	def dmax(n : Long) = { max = n } 
	def pp = p
}

class UploadController extends HttpServlet
{
  def message = "{ \"status\" : \"progress\", \"percentage\": "+ UploadController.value +" , \"link\" : \"google.com\" }"; 

  override def doGet(req : HSReq, resp : HSResp) =
  {
	resp.setHeader("Content-Type","application/json")
	resp.getWriter().print(message)	
  }
    
  override def doPost(req : HSReq, resp : HSResp) = {
	UploadController.clean	
	
	var is = req.getInputStream();
	val output = new FileOutputStream("/tmp/x")
	val buffer = new Array[ Byte ]( 8196 )
	var max = req.getIntHeader("Content-Length")
	
	UploadController.dmax(max)	
	
	var readed = 0
	do{
		var y= is.read(buffer,0, buffer.length)
		output.write(buffer,0,y)
		readed += y
		UploadController.incr(y)
	} while(max > readed);

	output.flush()
	output.close()

    resp.getWriter().print("ok" +  max + "," + UploadController.pp + "," + UploadController.value  + "," + req.getHeader("Content-Type"));
  }
}

abstract class BaseServlet extends HttpServlet
{
  import scala.collection.mutable.{Map => MMap}
  
  def message : scala.xml.Node;
  
  protected var param : Map[String, String] = Map.empty
  protected var header : Map[String, String] = Map.empty
  
  override def doPost(req : HSReq, resp : HSResp) =
  {
    // Extract parameters
    //
    val m = MMap[String, String]()
    val e = req.getParameterNames()
    while (e.hasMoreElements())
    {
      val name = e.nextElement().asInstanceOf[String]
      m += (name -> req.getParameter(name))
    }
    param = Map.empty ++ m
  
    resp.getWriter().print(message)
  }
}
class MessageController extends BaseServlet
{
  override def message =
    if (validate(param))
      <HTML>
        <HEAD><TITLE>upload</TITLE></HEAD>
        <BODY>
			<p>ok, <a href='{param("file")}'>download</a></p> 
			<p>message: {param("message")}</p>
		</BODY>
      </HTML>
    else
      <HTML>
        <HEAD><TITLE>Error!</TITLE></HEAD>
        <BODY>no file can be found</BODY>
      </HTML>
  
  def validate(p : Map[String, String]) : Boolean =
  {
    p foreach {
      case ("file", "") => return false
      case (_, _) => ()
    }
    true
  }

  def currentTime = java.util.Calendar.getInstance().getTime()
}
