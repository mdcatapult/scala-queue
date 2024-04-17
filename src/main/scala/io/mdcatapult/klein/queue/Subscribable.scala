/*
 * Copyright 2024 Medicines Discovery Catapult
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mdcatapult.klein.queue

import akka.stream.alpakka.amqp.QueueDeclaration

/**
 * An abstraction of a queue
 * name: name of the queue
 * durable: should the queue survive a server restart
 */
trait Subscribable {
  val name: String
  val durable: Boolean
  val queueDeclaration = QueueDeclaration(name).withDurable(
    durable
  )
}
