package cronish

import java.util.concurrent.{Executors, ScheduledExecutorService}

import akka.actor.ActorSystem
import cronish.dsl.{Cron, CronTask, Infinite, Scheduled, StopGap}

/**
 * Manages the scheduled tasks
 */
class CronishScheduler(threads: Int) {
  protected[cronish] val system: ActorSystem = ActorSystem("CronTasks")
  protected[cronish] val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(threads)

  private val crons = collection.mutable.ListBuffer[Scheduled]()

  def schedule(task: CronTask,
            definition: Cron,
            delay: Long = 15,
            stopper: StopGap = Infinite): Scheduled = {
    val ros = new Scheduled(task, definition, delay, stopper, this)
    crons += ros
    ros
  }

  @deprecated("Use Scheduled.stop instead")
  def destroy(old: Scheduled) = stop(old)

  /**
   * stop a scheduled task
   */
  def stop(old: Scheduled) = crons -= old

  @deprecated("Use Scheduled.shutdown instead")
  def destroyAll = shutdown()

  /**
   * shutdown all the scheduled tasks
   */
  def shutdown() = {
    crons foreach (_.stop)
    system.terminate()
    executor.shutdown()
  }

  /**
   * returns the list of active scheduled tasks
   */
  def active = crons.toList

  java.lang.Runtime.getRuntime().addShutdownHook(new Thread {
    override def run() { shutdown() }
  })
}
