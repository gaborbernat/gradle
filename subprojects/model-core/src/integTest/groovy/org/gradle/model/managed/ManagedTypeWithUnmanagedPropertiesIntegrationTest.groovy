/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.managed

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.EnableModelDsl

class ManagedTypeWithUnmanagedPropertiesIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        EnableModelDsl.enable(executer)
    }

    def "can have unmanaged property"() {
        when:
        buildScript '''
            import org.gradle.model.*
            import org.gradle.model.collection.*

            class UnmanagedThing {
              String value
            }

            @Managed
            interface ManagedThing {
                @Unmanaged
                UnmanagedThing getUnmanaged()
                void setUnmanaged(UnmanagedThing unmanaged)
            }

            class RulePlugin extends RuleSource {
                @Model
                void m(ManagedThing thing) {
                    thing.unmanaged = new UnmanagedThing(value: "foo")
                }

                @Mutate
                void addTask(ModelMap<Task> tasks, ManagedThing thing) {
                    tasks.create("echo") {
                        it.doLast {
                            println "value: $thing.unmanaged.value"
                        }
                    }
                }
            }

            apply type: RulePlugin
        '''

        then:
        succeeds "echo"

        and:
        output.contains('value: foo')
    }

    def "unmanaged property of managed type can be targeted by rules"() {
        when:
        buildScript '''
            import org.gradle.model.*
            import org.gradle.model.collection.*

            @Managed
            interface Platform {
                @Unmanaged
                OperatingSystem getOperatingSystem()
                void setOperatingSystem(OperatingSystem os)
            }

            class OperatingSystem {
                String name
            }

            class RulePlugin extends RuleSource {
                @Model
                void platform(Platform platform) {}

                @Mutate
                void setOs(Platform platform) {
                    platform.operatingSystem = new OperatingSystem()
                }

                @Mutate
                void setOsName(@Path("platform.operatingSystem") OperatingSystem os) {
                  os.name = "foo"
                }

                @Mutate
                void addTask(ModelMap<Task> tasks, @Path("platform.operatingSystem") OperatingSystem os) {
                  tasks.create("fromPlugin") {
                    doLast { println "fromPlugin: $os.name" }
                  }
                }
            }

            apply type: RulePlugin

            model {
                tasks {
                  create("fromScript") {
                    it.doLast { println "fromScript: " + $("platform.operatingSystem").name }
                  }
                }
            }
        '''

        then:
        succeeds "fromPlugin", "fromScript"

        and:
        output.contains("fromPlugin: foo")
        output.contains("fromScript: foo")
    }
}
