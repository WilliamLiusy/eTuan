package Global

object ServiceCenter {
  val projectName: String = "eTuan4"
  val dbManagerServiceCode = "A000001"
  val tongWenDBServiceCode = "A000002"
  val tongWenServiceCode = "A000003"

  val UserCenterCode = "A000010"
  val OrderServiceCode = "A000011"
  val ProductServiceCode = "A000012"
  val DispatcherServiceCode = "A000013"

  val fullNameMap: Map[String, String] = Map(
    tongWenDBServiceCode -> "DB-Manager（DB-Manager）",
    tongWenServiceCode -> "Tong-Wen（Tong-Wen）",
    UserCenterCode -> "UserCenter（UserCenter)",
    OrderServiceCode -> "OrderService（OrderService)",
    ProductServiceCode -> "ProductService（ProductService)",
    DispatcherServiceCode -> "DispatcherService（DispatcherService)"
  )

  def serviceName(serviceCode: String): String = {
    fullNameMap(serviceCode).toLowerCase
  }
}
