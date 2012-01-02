package com.example
 
import org.scalamock.annotation.{mock, mockObject}	
import javax.servlet.http.{ HttpServlet, HttpServletRequest => HSReq, HttpServletResponse => HSResp }
 
@mock[UplpipeServletInputStream]
class Dummy