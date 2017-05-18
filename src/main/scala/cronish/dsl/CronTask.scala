package cronish.dsl

import cronish.CronishScheduler

/**
 * a task that is can be scheduled
 *
 * @constructor creates a schedulable task
 * @param work         a function to be executed  on schedule
 * @param description  a description
 * @param startHandler a prehook to be executed before the scheduled function
 * @param errHandler   an exception handler to deal with the execution failure
 * @param exitHandler  a posthook to be executed after the scheduled function
 */
class CronTask(work: => Unit,
           val description: Option[String] = None,
           val startHandler: Option[Function0[Unit]] = None,
           val errHandler: Option[(Exception => Unit)] = None,
           val exitHandler: Option[Function0[Unit]] = None) extends Runnable {

  def run() = try {
    work
  } catch {
    case e: Exception =>
      errHandler.foreach(_.apply(e))
  }

  /**
   * Schedule the task to run. Alias for [[executes]].
   * @param definition has to be a valid [[Cron]] definition
   */
  def runs(definition: String)(implicit cronishScheduler: CronishScheduler): Scheduled = executes(definition)
  /**
   * Schedule the task to run. Alias for [[executes]].
   * @param definition when to execute the task
   */
  def runs(definition: Cron)(implicit cronishScheduler: CronishScheduler): Scheduled = executes(definition)

  /**
   * Schedule the task to run.
   * @param definition has to be a valid [[Cron]] definition
   */
  def executes(definition: String)(implicit cronishScheduler: CronishScheduler): Scheduled =
    executes(definition.cron)
  /**
   * Schedule the task to run.
   * @param definition when to execute the task
   */
  def executes(definition: Cron)(implicit cronishScheduler: CronishScheduler): Scheduled =
    cronishScheduler.schedule(this, definition)

  /**
   * provide a description and returns the modified task
   */
  def describedAs(something: String) =
    new CronTask(work, Some(something), startHandler, errHandler, exitHandler)

  /**
   * add a start prehook and returns the modified task
   */
  def starts(handler: => Unit) =
    new CronTask(work, description, Some(() => handler), errHandler, exitHandler)

  /**
   * add an execution handler and returns the modified task
   */
  def catches(handler: Exception => Unit) =
    new CronTask(work, description, startHandler, Some(handler), exitHandler)

  /**
   * add a finish posthook and returns the modified task
   */
  def ends(handler: => Unit) =
    new CronTask(work, description, startHandler, errHandler, Some(() => handler))
}
