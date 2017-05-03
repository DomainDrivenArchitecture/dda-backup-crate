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

(ns org.domaindrivenarchitecture.pallet.crate.backup
  (:require
   [schema.core :as s]
   [schema-tools.core :as st]
   [pallet.actions :as actions]
   [pallet.stevedore :as stevedore]
   [org.domaindrivenarchitecture.pallet.core.dda-crate :as dda-crate]
   [org.domaindrivenarchitecture.pallet.core.dda-crate.config :as internal-config]
   [org.domaindrivenarchitecture.config.commons.map-utils :as map-utils]
   [org.domaindrivenarchitecture.pallet.crate.backup.backup-element :as backup-element]
   [org.domaindrivenarchitecture.pallet.crate.backup.app :as app]
   [org.domaindrivenarchitecture.pallet.crate.backup.duplicity :as duplicity]))

(def BackupConfig
  "The configuration for backup crate."
  {:backup-name s/Str
   :script-path s/Str
   (s/optional-key :backup-user) app/User
   :gens-stored-on-source-system s/Num
   (s/optional-key :elements) [backup-element/BackupElement]
   (s/optional-key :service-restart) s/Str})

(def default-backup-config
  {:backup-name "backup"
   :script-path "/usr/lib/dda-backup/"
   :gens-stored-on-source-system 3})

(def backup-user {:backup-user {:name "dataBackupSource"
                                :encrypted-passwd "WIwn6jIUt2Rbc"}})

(def dda-backup-crate
  (dda-crate/make-dda-crate
   :facility :dda-backup
   :version [0 3 4]
   :config-default default-backup-config))

(s/defn ^:always-validate merge-config :- BackupConfig
  "merges the partial config with default config & ensures that resulting config is valid."
  [partial-config]
  (if (and (contains? partial-config :elements) (= (map :type (get partial-config :elements)) "duplicity"))
    (map-utils/deep-merge default-backup-config partial-config)
    ((map-utils/deep-merge (map-utils/deep-merge default-backup-config backup-user) partial-config))))

(defn install
  "collected install actions for backup crate."
  [partial-config]
  (let [config (merge-config partial-config) dup (= (map :type (get config :elements)) "duplicity")]
    (when dup (duplicity/install))
    (when (not dup) (app/create-backup-source-user (st/get-in config [:backup-user])))
    (when (not dup) (app/create-script-environment (st/get-in config [:script-path])))))

(defmethod dda-crate/dda-install (:facility dda-backup-crate) [dda-crate partial-config]
  (install partial-config))

(defn configure
  "collected configuration actions for backup crate."
  [partial-config]
  (let [config (merge-config partial-config) dup (= (map :type (get config :elements)) "duplicity")]
    (app/write-scripts config)
    (when dup
      (duplicity/configure))))

(defmethod dda-crate/dda-configure (:facility dda-backup-crate) [dda-crate partial-config]
  (configure partial-config))

(def with-backup
  (dda-crate/create-server-spec dda-backup-crate))
