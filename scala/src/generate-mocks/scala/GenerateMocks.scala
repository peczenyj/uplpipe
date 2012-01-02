package com.uplpipe
 
import org.scalamock.annotation.{mock, mockObject}
import javax.servlet.ServletInputStream

@mockObject(util)
@mock[Session]
@mock[ServletInputStream]
@mock[UplpipeServletInputStream]
class Dummy
