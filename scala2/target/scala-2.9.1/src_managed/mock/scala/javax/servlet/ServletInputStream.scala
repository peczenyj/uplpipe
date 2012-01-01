package javax.servlet;

class ServletInputStream(dummy: org.scalamock.MockConstructorDummy) extends java.io.InputStream with Mock$ServletInputStream {

  def readLine(x$1: Array[Byte], x$2: Int, x$3: Int): Int = if (forwardTo$Mocks != null) forwarder$0(x$1.asInstanceOf[AnyRef], x$2.asInstanceOf[AnyRef], x$3.asInstanceOf[AnyRef]).asInstanceOf[Int] else mock$0.handle(Array(x$1, x$2, x$3)).asInstanceOf[Int]
  def this() = {
    this(new org.scalamock.MockConstructorDummy)
    val mock = mock$1.handle(Array()).asInstanceOf[javax.servlet.ServletInputStream]
    if (mock != null) {
      forwardTo$Mocks = mock
    } else {
      val clazz = org.scalamock.ReflectionUtilities.getUnmockedClass(getClass, "javax.servlet.ServletInputStream")
      val constructor = clazz.getConstructor()
      forwardTo$Mocks = constructor.newInstance().asInstanceOf[AnyRef]
    }
  }
  override def markSupported(): Boolean = if (forwardTo$Mocks != null) forwarder$2().asInstanceOf[Boolean] else mock$2.handle(Array()).asInstanceOf[Boolean]
  override def reset(): Unit = if (forwardTo$Mocks != null) forwarder$3().asInstanceOf[Unit] else mock$3.handle(Array()).asInstanceOf[Unit]
  override def mark(x$1: Int): Unit = if (forwardTo$Mocks != null) forwarder$4(x$1.asInstanceOf[AnyRef]).asInstanceOf[Unit] else mock$4.handle(Array(x$1)).asInstanceOf[Unit]
  override def close(): Unit = if (forwardTo$Mocks != null) forwarder$5().asInstanceOf[Unit] else mock$5.handle(Array()).asInstanceOf[Unit]
  override def available(): Int = if (forwardTo$Mocks != null) forwarder$6().asInstanceOf[Int] else mock$6.handle(Array()).asInstanceOf[Int]
  override def skip(x$1: Long): Long = if (forwardTo$Mocks != null) forwarder$7(x$1.asInstanceOf[AnyRef]).asInstanceOf[Long] else mock$7.handle(Array(x$1)).asInstanceOf[Long]
  override def read(x$1: Array[Byte], x$2: Int, x$3: Int): Int = if (forwardTo$Mocks != null) forwarder$8(x$1.asInstanceOf[AnyRef], x$2.asInstanceOf[AnyRef], x$3.asInstanceOf[AnyRef]).asInstanceOf[Int] else mock$8.handle(Array(x$1, x$2, x$3)).asInstanceOf[Int]
  override def read(x$1: Array[Byte]): Int = if (forwardTo$Mocks != null) forwarder$9(x$1.asInstanceOf[AnyRef]).asInstanceOf[Int] else mock$9.handle(Array(x$1)).asInstanceOf[Int]
  override def read(): Int = if (forwardTo$Mocks != null) forwarder$10().asInstanceOf[Int] else mock$10.handle(Array()).asInstanceOf[Int]

  protected lazy val mock$0 = new org.scalamock.MockFunction(factory, Symbol("readLine"))
  protected lazy val mock$1 = new org.scalamock.MockConstructor[javax.servlet.ServletInputStream](factory, Symbol("this"))
  protected lazy val mock$2 = new org.scalamock.MockFunction(factory, Symbol("markSupported"))
  protected lazy val mock$3 = new org.scalamock.MockFunction(factory, Symbol("reset"))
  protected lazy val mock$4 = new org.scalamock.MockFunction(factory, Symbol("mark"))
  protected lazy val mock$5 = new org.scalamock.MockFunction(factory, Symbol("close"))
  protected lazy val mock$6 = new org.scalamock.MockFunction(factory, Symbol("available"))
  protected lazy val mock$7 = new org.scalamock.MockFunction(factory, Symbol("skip"))
  protected lazy val mock$8 = new org.scalamock.MockFunction(factory, Symbol("read"))
  protected lazy val mock$9 = new org.scalamock.MockFunction(factory, Symbol("read"))
  protected lazy val mock$10 = new org.scalamock.MockFunction(factory, Symbol("read"))

  private var forwardTo$Mocks: AnyRef = _

  private lazy val forwarder$0 = {
    val method = forwardTo$Mocks.getClass.getMethod("readLine", classOf[Array[Byte]], classOf[Int], classOf[Int])
    (x$1: AnyRef, x$2: AnyRef, x$3: AnyRef) => method.invoke(forwardTo$Mocks, x$1, x$2, x$3)
  }
  private lazy val forwarder$1 = {
    val method = forwardTo$Mocks.getClass.getMethod("<init>")
    () => method.invoke(forwardTo$Mocks)
  }
  private lazy val forwarder$2 = {
    val method = forwardTo$Mocks.getClass.getMethod("markSupported")
    () => method.invoke(forwardTo$Mocks)
  }
  private lazy val forwarder$3 = {
    val method = forwardTo$Mocks.getClass.getMethod("reset")
    () => method.invoke(forwardTo$Mocks)
  }
  private lazy val forwarder$4 = {
    val method = forwardTo$Mocks.getClass.getMethod("mark", classOf[Int])
    (x$1: AnyRef) => method.invoke(forwardTo$Mocks, x$1)
  }
  private lazy val forwarder$5 = {
    val method = forwardTo$Mocks.getClass.getMethod("close")
    () => method.invoke(forwardTo$Mocks)
  }
  private lazy val forwarder$6 = {
    val method = forwardTo$Mocks.getClass.getMethod("available")
    () => method.invoke(forwardTo$Mocks)
  }
  private lazy val forwarder$7 = {
    val method = forwardTo$Mocks.getClass.getMethod("skip", classOf[Long])
    (x$1: AnyRef) => method.invoke(forwardTo$Mocks, x$1)
  }
  private lazy val forwarder$8 = {
    val method = forwardTo$Mocks.getClass.getMethod("read", classOf[Array[Byte]], classOf[Int], classOf[Int])
    (x$1: AnyRef, x$2: AnyRef, x$3: AnyRef) => method.invoke(forwardTo$Mocks, x$1, x$2, x$3)
  }
  private lazy val forwarder$9 = {
    val method = forwardTo$Mocks.getClass.getMethod("read", classOf[Array[Byte]])
    (x$1: AnyRef) => method.invoke(forwardTo$Mocks, x$1)
  }
  private lazy val forwarder$10 = {
    val method = forwardTo$Mocks.getClass.getMethod("read")
    () => method.invoke(forwardTo$Mocks)
  }

  

}