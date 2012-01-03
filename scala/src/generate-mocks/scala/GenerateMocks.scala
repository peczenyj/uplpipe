package com.uplpipe
 
import org.scalamock.annotation.{mock, mockObject}
import javax.servlet.ServletInputStream

@mockObject(util)
@mockObject(session)
@mock[ServletInputStream]
@mock[UplpipeServletInputStream]
class Dummy
