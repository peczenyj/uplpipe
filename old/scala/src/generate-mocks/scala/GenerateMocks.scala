package com.example
 
import org.scalamock.annotation.{mock, mockObject}	
import javax.servlet.http.{ HttpServlet, HttpServletRequest => HSReq, HttpServletResponse => HSResp }
 
@mockObject(session)
@mock[UplpipeServletInputStream]
class Dummy