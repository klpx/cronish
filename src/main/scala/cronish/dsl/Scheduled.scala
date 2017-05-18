package cronish.dsl

import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent._

import akka.actor.{Actor, Props}
import cronish.{CronishScheduler, Logging}
import scalendar._
import scalendar.conversions._



/**
 * a scheduled [[CronTask]]. Provides hooks to control the schedule of that task (and stop it)
 */
class Scheduled private[cronish] (
    val task: CronTask,
    val definition: Cron,
    val initialDelay: Long,
    val stopGap: StopGap,
    cronishScheduler: CronishScheduler)  extends Logging { parent =>

  private val handler = cronishScheduler.system.actorOf(Props(new Handler))

  private case object Stop
  private case object Execute
  private case object ScheduleNextRun

  private class Handler extends Actor {
    var lastScheduled: Option[ScheduledFuture[_]] = None
    val sendExecute: Runnable = () => self ! Execute

    private def schedule(delayMillis: Long)(code: => Unit): Unit = {
      try {
        lastScheduled = None
        if (delayMillis <= 0) {
          code
        } else {
          lastScheduled = Some(
            cronishScheduler.executor.schedule((() => code): Runnable, delayMillis, MILLISECONDS)
          )
        }
      } catch {
        case e: RejectedExecutionException =>
          logger.warning(s"Scheduled executor is too busy to execute `${task.description.getOrElse("unnamed task")}`: ${e.getMessage}")
      }
    }

    override def preStart(): Unit =
      schedule(initialDelay) {
        task.startHandler.foreach(_.apply())
        self ! ScheduleNextRun
      }

    override def postStop(): Unit =
      lastScheduled.foreach(_.cancel(false))

    def receive: Receive = {
      case Stop => context.stop(self)
      case ScheduleNextRun =>
        schedule(definition.next) {
          self ! Execute
        }
      case Execute =>
        parent.stopGap.check match {
          case Some(_) =>
            parent.task.run()
            self ! ScheduleNextRun
          case None =>
            parent.stop()
        }
    }
  }

  /**
   * stop a scheduled task
   */
  def stop(): Unit = {
    task.exitHandler.foreach(_.apply())
    cronishScheduler.stop(parent)
    handler ! Stop
  }

  /**
   * reset a job to its definition
   */
  def reset(): Scheduled = preserve {
    cronishScheduler.schedule(task, definition, initialDelay, stopGap)
  }

  /**
   * control how many times this is executed
   */
  def exactly(stopper: StopGap): Scheduled = preserve {
    cronishScheduler.schedule(task, definition, initialDelay, stopper)
  }

  /**
   * set a deadline for the execution
   */
  def until(date: Scalendar): Scheduled = preserve {
    cronishScheduler.schedule(task, definition, initialDelay, new Timed(date))
  }

  /**
   * delay the first run of this scheduled task
   */
  def starting(date: Scalendar): Scheduled = preserve {
    val now = Scalendar.now
    val larger = if (date < now) now else date
    cronishScheduler.schedule(task, definition, larger.time - now.time, stopGap)
  }

  /**
   * delay the first run of this scheduled task by a fixed amount of milliseconds
   */
  def in(d: Long): Scheduled = preserve {
    cronishScheduler.schedule(task, definition, d, stopGap)
  }

  def in(d: Evaluated): Scheduled = preserve {
    cronishScheduler.schedule(task, definition, d.milliseconds, stopGap)
  }

  private def preserve[A](block: => A): A = {
    stop(); block
  }
}
