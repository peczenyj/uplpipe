import javax.servlet.http.{HttpServlet,
        HttpServletRequest => HSReq, 
        HttpServletResponse => HSResp
}

import javax.servlet.annotation.{
        MultipartConfig,
        WebServlet
}

@WebServlet(name = "UploadServlet", urlPatterns = Array("/upload/*"))
@MultipartConfig(maxFileSize=1024*1024*50, maxRequestSize=1024*1024*50)
class HelloWorldServlet extends HttpServlet {
	override def doGet(req: HSReq, resp: HSResp) = {
		resp.getWriter().print("Hello World!")
	}
}