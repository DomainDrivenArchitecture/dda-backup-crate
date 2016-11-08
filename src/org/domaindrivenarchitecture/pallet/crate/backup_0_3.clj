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

(ns org.domaindrivenarchitecture.pallet.crate.backup-0-3
   (:require
     [schema.core :as s]
     [schema-tools.core :as st]
     [pallet.actions :as actions]
     [pallet.stevedore :as stevedore]
     [org.domaindrivenarchitecture.pallet.crate.config-0-3 :as config]
     [org.domaindrivenarchitecture.config.commons.map-utils :as map-utils]
     [org.domaindrivenarchitecture.pallet.crate.backup.backup-element :as backup-element]
     [org.domaindrivenarchitecture.pallet.crate.backup.app :as app]
     ))

(def facility
  :dda-backup)

(def BackupConfig
  "The configuration for backup crate." 
  {:backup-name s/Str
   (s/optional-key :service-restart) s/Str
   :script-path s/Str
   :backup-user app/User
   :gens-stored-on-source-system s/Num
   :elements [backup-element/BackupElement]})


(s/defn default-backup-config
  "The default backup configuration."
  []
  {:backup-user {:name "dataBackupSource"
                 :encrypted-passwd "WIwn6jIUt2Rbc"}
   :script-path "/usr/lib/dda-backup/"
   :gens-stored-on-source-system 3})

(s/defn ^:always-validate merge-config :- BackupConfig
  "merges the partial config with default config & ensures that resulting config is valid."
  [partial-config]
  (map-utils/deep-merge (default-backup-config) partial-config))

(defn install
  "collected install actions for backup crate."
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    (app/create-backup-source-user (st/get-in config [:backup-user])) 
    (app/create-script-environment (st/get-in config [:script-path]))
  ))

(defn configure
  "collected configuration actions for backup crate."
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    (app/write-scripts config)
  ))