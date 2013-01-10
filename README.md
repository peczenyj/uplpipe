## Uplpipe - yet another upload pipe

Upload Pipe is the name of this Proof of Concept. It is a upload progress bar server side with integration with Amazon S3.

This project has two components:
 - Uplpipe Server, a Servlet written in Scala 
 - Uplpipe::Agent, a perl daemon who store files in S3

### Uplpipe Server 

You can find the code in scala directory.

#### Install and usage

1) install scala and sbt 

In Mac OS X, using brew, is possible do this:

  bash$ brew install scala   
  bash$ brew install sbt

in others systems please follow this:

 - http://www.scala-lang.org/downloads
 - http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html
 
2) enter in the scala directory (uplpipe project) and type sbt
<pre>
  bash$ cd /path/to/uplpipe
  bash$ cd scala
  bash$ sbt
</pre>  
the sbt is like 'make' for scala projects, but more powerfull. in the first execution will download all internet using maven. wait some minutes

3) in the sbt console, use the command 'compile', the message success will be printed.

<pre>
> compile

[info] Compiling 1 Scala source to /Users/peczenyj/Documents/workspace/uplpipe/scala/target/scala-2.9.1/classes...
[info] 'compiler-interface' not yet compiled for Scala 2.9.1.final. Compiling...
[info]   Compilation completed in 40.252 s
[success] Total time: 47 s, completed 07/01/2013 23:44:57
</pre>

4) now, do the command 'package'

<pre>
> package
</pre>

5) and finally, run 'container:start'
<pre>
>container:start
2013-01-08 00:25:39.775:INFO::Logging to STDERR via org.mortbay.log.StdErrLog
[info] jetty-6.1.22
[info] NO JSP Support for /, did not find org.apache.jasper.servlet.JspServlet
[info] Started SelectChannelConnector@0.0.0.0:8080
[success] Total time: 0 s, completed 08/01/2013 00:25:40
</pre>

6) go to http://127.0.0.1:8080/ with any modern browser, try to upload something BIG, it will be stored in /tmp

7) to quit:

<pre>
> container:stop
> exit
</pre>

### Tests

To run tests

<pre>
> generate-mocks
> test
</pre>

should be like this:

<pre>
[info] UplpipeServletInputStreamTest:
[info] - read chunk of data
[info] - read simple line
[info] - read bytes until max value
[info] - read great line - length 2x chunk
[info] UplpipeMultipartRequestParserTest:
[info] - test simple with no boundary
[info] - test simple with boundary but improper values in header [content type]
[info] - test simple with boundary but no name in header [content disposition]
[info] - should return None if fieldname not found
[info] - should return a valid file
[info] - should throw a exception if truncate fails
[info] - should throw a exception if rename fails
[info] UplpipeWebAppTest:
[info] - expect throw error if not multipart/form-data request
[info] - Should receive a correct file with fieldname incorrect and throws exception
[info] - Should receive a correct file
[info] Passed: : Total 14, Failed 0, Errors 0, Passed 14, Skipped 0
[success] Total time: 35 s, completed 08/01/2013 00:32:00
</pre>
