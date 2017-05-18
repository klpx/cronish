package cronish.dsl

import scalendar.Scalendar

sealed trait StopGap {
  def check: Option[Int]
  def times(limit: Int) = new Limited(limit)
}

class Limited(initial: Int) extends StopGap {
  private var limit = initial

  require(limit > 0)

  def check = {
    limit -= 1
    if (limit == 0) None else Some(limit)
  }
}

class Timed(limit: Scalendar) extends StopGap {
  def check = {
    if (limit < Scalendar.now) None else Some(1)
  }
}

case object Infinite extends StopGap {
  def check = Some(1)
}
