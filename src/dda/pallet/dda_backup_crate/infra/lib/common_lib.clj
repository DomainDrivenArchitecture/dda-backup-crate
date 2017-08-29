; Licensed to the Apache Software Foundation (ASF) under one
; or more contributor license agreements. See the NOTICE file
; distributed with this work for additional information
; regarding copyright ownership. The ASF licenses this file
; to you under the Apache License, Version 2.0 (the
; "License"); you may not use this file except in compliance
; with the License. You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.


(ns dda.pallet.dda-backup-crate.infra.lib.common-lib)

(defn prefix
  "prefixes each string contained in lines with indent."
  ([indent lines]
    (prefix indent lines ())
  )
  ([indent lines result]
    {:pre [(list? result)]}
    (if (empty? lines)
      (into [] result)
      (recur
        indent
        (pop lines)
        (conj result
              (str indent
                   (peek lines)))
              )
      ))
  )


(def head
  ["#!/bin/bash"
   ""])

(def export-timestamp
  ["#timestamp from server to variable"
   "export timestamp=`date +%Y-%m-%d_%H-%M-%S`"
   ""])

(defn stop-app-server
  ""
  [service]
  ["#stop appserver"
   (str "service " service " stop")
   ""])

(defn start-app-server
  [service]
  ["#start appserver"
   (str "service " service " start")
   ""])
