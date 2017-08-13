package com.latamautos.kernel.scala.application.enums

import scala.concurrent.duration._

object EnumCommandBus {
  final var MAXIMUM_RETRIES: Int                      = 10
  final var MAXIMUM_RETRIES_VALIDATION: Int           = 0
  final var MAXIMUM_NUMBER_ATTEMPTS_REACHED: String   = "Maximum number of attempts reached"
  final var MAIN_TIMEOUT: FiniteDuration              = 60.seconds
  final var SUPERVISED_TIMEOUT: FiniteDuration        = 60.seconds
  final var EXCEPTION_NO_HANDLER_MESSAGE              = "No handler for %s"
  final var LOG_INFO_COMMANDBUS_ACTOR_MSG             = "CommandBusActorMsg"
  final var LOG_DEBUG_SUPERVISOR_FUTURE_SUCCESS       = "[CommandBusSupervisor]==========>>>>Future SUCCESS: %s"
  final var LOG_DEBUG_SUPERVISOR_FUTURE_FAILURE       = "[CommandBusSupervisor]==========>>>>Future FAILED: %s"
  final var LOG_DEBUG_RECEIVE_RESPONSE                = "[CommandBusSupervisor]==========>>>>Sending message: [%s] to OriginalSender: [%s]"
  final var LOG_DEBUG_TRYING_MESSAGE                  = "[CommandBusSupervisor]==========>>>>Trying message: [%s]"
  final var LOG_DEBUG_MAX_RETRIES_ATTEMPTS_REACHED_MESSAGE = "[CommandBusSupervisor]==========>>>>$EnumCommandBus.MAXIMUM_NUMBER_ATTEMPTS_REACHED: [%s]"
}
