import javax.servlet.http.{HttpServlet,
  HttpServletRequest => HSReq, HttpServletResponse => HSResp}

object UploadScalaServlet {
	var p = 0;
	def value = {
		p += 10
		p
	}
}

class UploadScalaServlet extends HttpServlet
{
  def message = "{ \"status\" : \"progress\", \"percentage\": "+ UploadScalaServlet.value +" , \"link\" : \"google.com\" }"; 

  def value = 50

  override def doGet(req : HSReq, resp : HSResp) =
  {
	resp.setHeader("Content-Type","application/json")
	resp.getWriter().print(message)	
  }
    
  override def doPost(req : HSReq, resp : HSResp) =
    resp.getWriter().print(req.getPart("f").getSize())
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
class NamedHelloWorldServlet extends BaseServlet
{
  override def message =
    if (validate(param))
      <HTML>
        <HEAD><TITLE>Hello!</TITLE></HEAD>
        <BODY>Hello, {param("firstName")} {param("lastName")}! It is now {currentTime}.</BODY>
      </HTML>
    else
      <HTML>
        <HEAD><TITLE>Error!</TITLE></HEAD>
        <BODY>How can we be friends if you don't tell me your name?!?</BODY>
      </HTML>
  
  def validate(p : Map[String, String]) : Boolean =
  {
    p foreach {
      case ("firstName", "") => return false
      case ("lastName", "") => return false
      //case ("lastName", v) => if (v.contains("e")) return false
      case (_, _) => ()
    }
    true
  }

  def currentTime = java.util.Calendar.getInstance().getTime()
}
