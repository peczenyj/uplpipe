package com.uplpipe

class UplpipeServletInputStream( 
	maxBytes :Int,
	inputReadLine: (Array[Byte], Int, Int) => Int, 
	onUpdate :Int => Unit) {
	
	var readedBytes = 0 
	
	def readLine(chunk :Int = 8 * 1024):String = {
		var result = 0
		val buf = new Array[Byte](chunk)
		val sbuf = new StringBuffer()
		
		do {
			result = readLine(buf, 0, buf.length)
			if (result != -1) sbuf.append(new String(buf, 0, result))
		}while (result == buf.length)
		
	    sbuf.toString.stripLineEnd
	}
	
 	def readLine(b :Array[Byte], off:Int, len:Int): Int = 
		if (maxBytes > readedBytes) {
			var result = inputReadLine(b, off, len)
			readedBytes += result 
			onUpdate(readedBytes)
			
			result
	    } else {
			-1 // update complete!!!
		}	
}