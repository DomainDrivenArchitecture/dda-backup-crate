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
(ns dda.pallet.dda-backup-crate.infra
  (:require
   [schema.core :as s]
   [schema-tools.core :as st]
   [pallet.actions :as actions]
   [pallet.stevedore :as stevedore]
   [dda.pallet.core.dda-crate :as dda-crate]
   [dda.pallet.core.dda-crate.config :as internal-config]
   [dda.config.commons.map-utils :as map-utils]
   [dda.pallet.dda-backup-crate.infra.core.backup-element :as backup-element]
   [dda.pallet.dda-backup-crate.infra.core.backup :as backup]
   [dda.pallet.dda-backup-crate.infra.duplicity.duplicity :as duplicity]))

(def facility :dda-backup)
(def version  [0 3 4])

;TODO: in the case of duplicity only one backup-element is supported, change to multiple
(def BasicBackupConfig
  {:backup-name s/Str
   :script-path s/Str
   :gens-stored-on-source-system s/Num
   (s/optional-key :elements) [backup-element/BackupElement]
   (s/optional-key :service-restart) s/Str})

(def BackupConfig
  "The configuration for backup crate."
  (merge {:backup-user backup/User} BasicBackupConfig))

(def default-backup-config
  {:backup-name "backup"
   :script-path "/usr/lib/dda-backup/"
   :gens-stored-on-source-system 3})

(def dda-backup-crate
  (dda-crate/make-dda-crate
   :facility facility
   :version version
   :config-default default-backup-config))

(s/defn ^:always-validate merge-config :- BackupConfig
  "merges the partial config with default config & ensures that resulting config is valid."
  [partial-config]
  (map-utils/deep-merge default-backup-config partial-config))

(defn install
  "collected install actions for backup crate."
  [partial-config]
  (let [config (merge-config partial-config) dup (duplicity/check-for-dup partial-config)]
    (if dup
      (duplicity/install)
      (do (backup/create-backup-directory (st/get-in config [:backup-user]))
          (backup/create-script-environment (st/get-in config [:script-path]))))))

(defmethod dda-crate/dda-install (:facility dda-backup-crate) [dda-crate partial-config]
  (install partial-config))

(defn configure
  "collected configuration actions for backup crate."
  [partial-config]
  (let [config (merge-config partial-config) dup (duplicity/check-for-dup partial-config)]
    (backup/write-scripts config)
    (when dup
      (duplicity/configure config))))

(defmethod dda-crate/dda-configure (:facility dda-backup-crate) [dda-crate partial-config]
  (configure partial-config))

(def with-backup
  (dda-crate/create-server-spec dda-backup-crate))
