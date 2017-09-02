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

(ns dda.pallet.dda-backup-crate.app.instantiate-existing
  (:require
   [clojure.inspector :as inspector]
   [pallet.repl :as pr]
   [dda.pallet.commons.session-tools :as session-tools]
   [dda.pallet.commons.pallet-schema :as ps]
   [dda.cm.operation :as operation]
   [dda.cm.existing :as existing]
   [dda.pallet.dda-backup-crate.app :as app]))

(def provisioning-ip
  "192.168.56.103")

(def provisioning-user
  {:login "initial"
   :password "test1234"})

(def domain-config {:backup-name "ssh"
                    :script-path "/usr/lib/dda-backup/"
                    :gens-stored-on-source-system 3
                    :elements [{:type :file-compressed
                                :name "ssh"
                                :root-dir "/etc/"
                                :subdir-to-save "ssh"}]})

(defn provider []
  (existing/provider provisioning-ip "node-id" "dda-backup-group"))

(defn integrated-group-spec []
  (merge
   (app/backup-group-spec (app/app-configuration domain-config))
   (existing/node-spec provisioning-user)))

(defn apply-install []
  (pr/session-summary
   (operation/do-apply-install (provider) (integrated-group-spec))))

(defn apply-config []
  (pr/session-summary
   (operation/do-apply-configure (provider) (integrated-group-spec))))

(defn server-test []
  (pr/session-summary
   (operation/do-server-test (provider) (integrated-group-spec))))
