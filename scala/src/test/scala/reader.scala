package com.uplpipe
 
import org.jmock.{Mockery,Expectations}
import org.jmock.lib.legacy.ClassImposteriser
import org.jmock.Expectations._

import org.scalatest.FunSuite
import org.scalamock.scalatest.MockFactory
import org.scalamock.generated.GeneratedMockFactory

import java.io.{File,FileReader,BufferedReader, FileOutputStream}
import javax.servlet.ServletInputStream

class UplpipeWebAppTest extends FunSuite with MockFactory with GeneratedMockFactory {

	def fixture_lines:Array[String] = {
		val lines=  new Array[String](8)
		lines(0) = "------------------------------c65180f21129\r\n"
		lines(1) = "Content-Disposition: form-data; name=\"f\"; filename=\"test.txt\"\r\n"
		lines(2) = "Content-Type: text/plain\r\n" 
		lines(3) = "\r\n"
		lines(4) = "12345\n"
		lines(5) = "abcde\n"
		lines(6) = "\r\n" 
		lines(7) = "------------------------------c65180f21129--"
		lines.clone
	}

	test("expect throw error if not multipart/form-data request"){
		val lines=  fixture_lines
		
		val uuid     = "abcdabcdabc9-test.txt"
		val content_type = "multipart/other"
		val content_length = 193
		
		val ses = mockObject(session)
		val app = new UplpipeWebApp
		
		ses.expects.setError(uuid).once
		
		val input   = mock[ServletInputStream]
		withClue("should throw an exception") {
		  intercept[IllegalArgumentException] {
		   	app.handleUpload(uuid,content_length, content_type, input)
		  }
		}	
	}	

	test("Should receive a correct file with fieldname incorrect and throws exception"){
		val lines=  fixture_lines
		
		lines(1) = "Content-Disposition: form-data; name=\"g\"; filename=\"test.txt\"\r\n"
		
		val uuid     = "abcdabcdabc9-test.txt"
		val content_type = "multipart/form-data; boundary=----------------------------c65180f21129"
		val content_length = 193
		 
		val input   = mock[ServletInputStream]		
		val ses = mockObject(session)
		val app = new UplpipeWebApp
		
		def _show(i:Int,b :Array[Byte]):Int = {
			val bytes = lines(i).getBytes
			Array.copy(bytes,0,b,0,bytes.length)
			bytes.length
		}
		
		inSequence {
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(0,b) })		
			ses.expects.setProgress(uuid,100.0 *44.0/193.0)	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(1,b) })		
			ses.expects.setProgress(uuid,100.0 *107.0/193.0)		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(2,b) })		
			ses.expects.setProgress(uuid,100.0 *133.0/193.0)	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(3,b) })		
			ses.expects.setProgress(uuid,100.0 *135.0/193.0)
			ses.expects.setError(uuid)								
		}
		
		withClue("should throw an exception") {
		  intercept[Exception] {
		   	val file = app.handleUpload(uuid,content_length, content_type, input)
		  }
		}		
	}
	def k(a:Int, b:Int):Double = 100.0*a.toDouble/b.toDouble
	
	test("Should receive a correct file"){
		val lines=  fixture_lines
		
		val uuid     = "abcdabcdabc9-test.txt"
		val content_type = "multipart/form-data; boundary=----------------------------c65180f21129"
		val content_length = 193
		 
		val input   = mock[ServletInputStream]		
		val ses = mockObject(session)
		val app = new UplpipeWebApp
		
		def _show(i:Int,b :Array[Byte]):Int = {
			val bytes = lines(i).getBytes
			Array.copy(bytes,0,b,0,bytes.length)
			bytes.length
		}
		
		inSequence {
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(0,b) })		
			ses.expects.setProgress(uuid,k(44,193)	)
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(1,b) })		
			ses.expects.setProgress(uuid,k(107,193)	)	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(2,b) })		
			ses.expects.setProgress(uuid,k(133,193)	)
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(3,b) })		
			ses.expects.setProgress(uuid,k(135,193)	)	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(4,b) })		
			ses.expects.setProgress(uuid,k(141,193)	)	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(5,b) })		
			ses.expects.setProgress(uuid,k(147,193)	)
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(6,b) })		
			ses.expects.setProgress(uuid,k(149,193))
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(7,b) })		
			ses.expects.setProgress(uuid,k(193,193))
			ses.expects.setComplete(uuid)									
		}
		
		app.handleUpload(uuid,content_length, content_type, input)		
		
		expect(true, "should be correctly saved in local storage") { 
			new File("/tmp/abcdabcdabc9-test.txt").exists 
		}		
	}
}

class UplpipeMultipartRequestParserTest extends FunSuite with MockFactory with GeneratedMockFactory {

	def fixture_lines:Array[String] = {
		val lines=  new Array[String](8)
		lines(0) = "------------------------------c65180f21129\r\n"
		lines(1) = "Content-Disposition: form-data; name=\"f\"; filename=\"test.txt\"\r\n"
		lines(2) = "Content-Type: text/plain\r\n" 
		lines(3) = "\r\n"
		lines(4) = "12345\n"
		lines(5) = "abcde\n"
		lines(6) = "\r\n" 
		lines(7) = "------------------------------c65180f21129--"
		lines.clone
	}
	
	test("test simple with no boundary") {
		val uuid     = "abcdabcdabcd-test.txt"
		val boundary = "----------------------------c65180f21129"
		val input    = mock[UplpipeServletInputStream]
		
		input.expects.readLine.returns("coco").once
		
		withClue("should throw an exception") {
		  intercept[IllegalArgumentException] {
		   	new UplpipeMultipartRequestParser(uuid,boundary,input)
		  }
		}
	}

	test("test simple with boundary but improper values in header [content type]") {
		val lines = fixture_lines
		lines(2) = "\r\n"	
		val uuid     = "abcdabcdabcd-test.txt"
		val boundary = "------------------------------c65180f21129"
		
		val input    = mock[UplpipeServletInputStream]
		inSequence {
			input.expects.readLine.returns(lines(0).stripLineEnd).once	
			input.expects.readLine.returns(lines(1).stripLineEnd).once
			input.expects.readLine.returns(lines(2).stripLineEnd).once
			input.expects.readLine.returns(lines(3).stripLineEnd).once
		}
		
		val parser = new UplpipeMultipartRequestParser(uuid,boundary,input)
		withClue("should throw an exception") {
		  intercept[IllegalArgumentException] {
		   	parser.getFileFromInput("f")
		  }
		}
	}

	test("test simple with boundary but no name in header [content disposition]") {
		val lines = fixture_lines
		lines(1) = "Content-Disposition: form-data; ...\r\n"	

		val uuid     = "abcdabcdabcd-test.txt"
		val boundary = "------------------------------c65180f21129"
		val input    = mock[UplpipeServletInputStream]
		inSequence {
			input.expects.readLine.returns(lines(0).stripLineEnd).once	
			input.expects.readLine.returns(lines(1).stripLineEnd).once
			input.expects.readLine.returns(lines(2).stripLineEnd).once
			input.expects.readLine.returns(lines(3).stripLineEnd).once
		}
		
		val parser = new UplpipeMultipartRequestParser(uuid,boundary,input)

		withClue("should throw an exception") {
		  intercept[IllegalArgumentException] {
		   	parser.getFileFromInput("f")
		  }
		}
	}

	test("should return None if fieldname not found"){
		val lines = fixture_lines

		val uuid     = "abcdabcdabcd-test.txt"
		val boundary = "------------------------------c65180f21129"

		val input    = mock[UplpipeServletInputStream]
		inSequence {
			input.expects.readLine.returns(lines(0).stripLineEnd).once	
			input.expects.readLine.returns(lines(1).stripLineEnd).once
			input.expects.readLine.returns(lines(2).stripLineEnd).once
			input.expects.readLine.returns(lines(3).stripLineEnd).once
		}

		val parser = new UplpipeMultipartRequestParser(uuid,boundary,input)

		val file = parser.getFileFromInput("g").flatMap({(f:File) => 
			fail("should not return a file instance :" + f)
		})
	}
	
	test("should return a valid file"){
		val lines    = fixture_lines 

		val uuid     = "abcdabcdabcd-test.txt"
		val boundary = "------------------------------c65180f21129"

		val input    = mock[UplpipeServletInputStream]
		
		def _show(i:Int,b :Array[Byte]):Int = {
			val bytes = lines(i).getBytes
			Array.copy(bytes,0,b,0,bytes.length)
			bytes.length
		}
		
		inSequence {
			input.expects.readLine.returns(lines(0).stripLineEnd).once	
			input.expects.readLine.returns(lines(1).stripLineEnd).once
			input.expects.readLine.returns(lines(2).stripLineEnd).once
			input.expects.readLine.returns(lines(3).stripLineEnd).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(4,b) }).once	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(5,b) }).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(6,b) }).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(7,b) }).once									
		}
		
		val parser = new UplpipeMultipartRequestParser(uuid,boundary,input)
		
		val file = parser.getFileFromInput("f").getOrElse { fail("file not found")}
		
		expect(12, "should have 12 bytes") { 
			val fos  = new FileOutputStream(file,true)
			val chan = fos.getChannel()
			chan.size 
		}
	}

	test("should throw a exception if truncate fails"){
		val lines    = fixture_lines 

		val uuid     = "abcdabcdabcd-test.txt"
		val boundary = "------------------------------c65180f21129"

		val myUtil   = mockObject(util)
		val input    = mock[UplpipeServletInputStream]
		
		def _show(i:Int,b :Array[Byte]):Int = {
			val bytes = lines(i).getBytes
			Array.copy(bytes,0,b,0,bytes.length)
			bytes.length
		}
		
		inSequence {
			input.expects.readLine.returns(lines(0).stripLineEnd).once	
			input.expects.readLine.returns(lines(1).stripLineEnd).once
			input.expects.readLine.returns(lines(2).stripLineEnd).once
			input.expects.readLine.returns(lines(3).stripLineEnd).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(4,b) }).once	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(5,b) }).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(6,b) }).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(7,b) }).once
			myUtil.expects.truncate(*).returns(false).once									
		}
		
		val parser = new UplpipeMultipartRequestParser(uuid,boundary,input)
		
		withClue("should throw an exception") {
		  intercept[Exception] {
		   	parser.getFileFromInput("f")
		  }
		}		
	}
	
	test("should throw a exception if rename fails"){
		val lines    = fixture_lines 

		val uuid     = "abcdabcdabcd-test.txt"
		val boundary = "------------------------------c65180f21129"

		val myUtil   = mockObject(util)
		val input    = mock[UplpipeServletInputStream]
		
		def _show(i:Int,b :Array[Byte]):Int = {
			val bytes = lines(i).getBytes
			Array.copy(bytes,0,b,0,bytes.length)
			bytes.length
		}
		
		inSequence {
			input.expects.readLine.returns(lines(0).stripLineEnd).once	
			input.expects.readLine.returns(lines(1).stripLineEnd).once
			input.expects.readLine.returns(lines(2).stripLineEnd).once
			input.expects.readLine.returns(lines(3).stripLineEnd).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(4,b) }).once	
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(5,b) }).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(6,b) }).once		
			input.expects.readLine(*,0,8*1024).onCall({ (b :Array[Byte], off:Int, len:Int) => _show(7,b) }).once
			myUtil.expects.truncate(*).returns(true).once	
			myUtil.expects.rename(*).returns(false,new File("/dev/null")).once
		}
		
		val parser = new UplpipeMultipartRequestParser(uuid,boundary,input)
		
		withClue("should throw an exception") {
		  intercept[Exception] {
		   	parser.getFileFromInput("f")
		  }
		}		
	}	
}


class UplpipeServletInputStreamTest extends FunSuite with MockFactory with GeneratedMockFactory {

	test("read chunk of data") {
		val input    = mock[ServletInputStream]
		val listener = mockFunction[Int,Unit]
		val content  = "12345678\r\n"
		val bytes    = content.getBytes
		val size     = bytes.length
		val chunk    = 8*1024
		
		input.expects.readLine(*,0,chunk).onCall({ 
			(b :Array[Byte], off:Int, len:Int) => Array.copy(bytes,0,b,0,size)

			size
		})
		
		listener.expects(size)
		
		val reader   = new UplpipeServletInputStream(size, input, { (readed :Int) => listener(readed) })
		
		val buff = new Array[Byte](chunk)
		val line = reader.readLine(buff, 0, buff.length)
		assert(line == size, "should be equal")
	}
 
	test("read simple line") {
		val input    = mock[ServletInputStream]
		val listener = mockFunction[Int,Unit]
		val content  = "test\r\n"
		val bytes    = content.getBytes
		val size     = bytes.length
		val chunk    = 8*1024
	
		inSequence{
			input.expects.readLine(*,0,8*1024).onCall({ 
				(b :Array[Byte], off:Int, len:Int) => Array.copy(bytes,0,b,0,size)

				size
			})

			listener.expects(size)
		}
		
		val reader   = new UplpipeServletInputStream(size, input, { 
			(readed :Int) => listener(readed)
		})
		
		val line = reader.readLine
		assert(line == content.stripLineEnd, "should be equal")
	} 

	test("read bytes until max value"){
		val input    = mock[ServletInputStream]
		val listener = mockFunction[Int,Unit]
		val content  = "test\r\n"
		val bytes    = content.getBytes
		val size     = bytes.length
		val chunk    = 8*1024
	
		inSequence{
			input.expects.readLine(*,0,8*1024).onCall({ 
				(b :Array[Byte], off:Int, len:Int) => Array.copy(bytes,0,b,0,size)

				size
			})

			listener.expects(size)
		}
		
		val reader   = new UplpipeServletInputStream(size, input, { 
			(readed :Int) => listener(readed)
		})
		
		val line = reader.readLine
		assert(line == content.stripLineEnd, "should be equal")
	} 
	
	test("read great line - length 2x chunk") {
		val input    = mock[ServletInputStream]
		val listener = mockFunction[Int, Unit]
		val content  = "something very large\r\n"
		val bytes    = content.getBytes
		val size     = bytes.length
		val half     = size/2
		val chunk    = half
		
		inSequence {
			input.expects.readLine(*,0,chunk).onCall({ 
				(b :Array[Byte], off:Int, len:Int) => Array.copy(bytes,0,b,0,half)

				half
			})
		
			listener.expects(half)

			input.expects.readLine(*,0,chunk).onCall({ 
				(b :Array[Byte], off:Int, len:Int) => Array.copy(bytes,half,b,0,half)
			
				half
			})
		
			listener.expects(size)		
		}
		
		val reader   = new UplpipeServletInputStream(size,input,{ 
			(readed :Int) => listener(readed)
		},chunk)
		
		val line = reader.readLine
		assert(line == content.stripLineEnd, "should be equal")
	}	
}