package com.hastybox.delayedwebhooks.core

case class WebHookCall(
                        delay: Option[Int],
                        event: String,
                        key: String
                      )
