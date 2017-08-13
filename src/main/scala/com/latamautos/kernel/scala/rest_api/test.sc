import scala.collection.immutable.::
import org.springframework.data.domain.Sort.{Order, Direction}
val a: List[String] = List("price,asc", "mileage,desc", "year,", ",asc", "", ",")
//!x.isEmpty
val a2 = a.filter(x =>
  x.split(",").toList match {

    case Nil => false
    case i :: Nil =>
      !i.isEmpty && (i.equals(x.trim)||(i+",").equals(x.trim))

    case i :: xs =>
      !i.isEmpty && xs.size==1
  }).map(x => {
  val propertyDirection = x.split(",")
  val (property: String, direction: Option[String] )=
    (propertyDirection(0), if (propertyDirection.length>1) Some(propertyDirection(1)) else None)
  new Order(Direction.fromStringOrNull(direction.orNull), property)
})