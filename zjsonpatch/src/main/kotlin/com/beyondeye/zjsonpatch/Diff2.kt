/*
 * Copyright 2016 flipkart.com zjsonpatch.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.beyondeye.zjsonpatch

import com.google.gson.JsonElement

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
internal class Diff2 {
    val operation: Int
    val path: MutableList<Any>
    val value: JsonElement?
    val toPath: List<Any>? //only to be used in move operation

    constructor(operation: Int, path: List<Any>, value: JsonElement) {
        this.operation = operation
        this.path = path.toMutableList()
        this.toPath=null
        this.value = value
    }

    constructor(operation: Int, fromPath: List<Any>, toPath: List<Any>) {
        this.operation = operation
        this.path = fromPath.toMutableList()
        this.toPath = toPath
        this.value = null
    }

    companion object {

        @JvmStatic fun generateDiff(replace: Int, path: List<Any>, target: JsonElement): Diff2 {
            return Diff2(replace, path, target)
        }
    }
}