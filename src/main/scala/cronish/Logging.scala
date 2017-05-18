package cronish

import java.util.logging.Logger

trait Logging {
  protected val logger = Logger.getLogger(getClass.getName)
}

