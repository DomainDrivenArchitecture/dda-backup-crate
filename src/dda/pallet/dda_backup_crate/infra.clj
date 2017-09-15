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
   [dda.pallet.core.dda-crate :as dda-crate]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]
   [dda.pallet.dda-backup-crate.infra.backup-elements :as elements]
   [dda.pallet.dda-backup-crate.infra.local-management :as local]))

(def facility :dda-backup)
(def version  [0 3 4])

(def BackupConfig schema/BackupConfig)

(def InfraResult {facility BackupConfig})

(def dda-backup-crate
  (dda-crate/make-dda-crate
   :facility facility
   :version version
   :config-default {}))

(s/defn ^:always-validate install
  "collected install actions for backup crate."
  [config :- BackupConfig]
  (let [{:keys [backup-user backup-script-path backup-store-folder
                transport-management local-management]} config]
    (local/create-backup-directory backup-user backup-store-folder)
    (local/create-script-environment backup-script-path)))
    ;(when (contains? transport-management :duplicity-push)
    ;  (duplicity/install))))

(s/defn ^:always-validate configure
  "collected configuration actions for backup crate."
  [config :- BackupConfig]
  (let [{:keys [backup-name backup-script-path
                backup-store-folder service-restart
                backup-user backup-elements]} config]
    (elements/write-file backup-name :backup backup-script-path "10_"
                         (elements/backup-script-lines backup-name backup-store-folder
                                                       service-restart (name backup-user) backup-elements))))
    ;(when (contains? transport-management :duplicity-push)
    ;  (duplicity/configure config)))

(defmethod dda-crate/dda-install facility [dda-crate config]
  (install config))

(defmethod dda-crate/dda-configure facility [dda-crate config]
  (configure config))

(def with-backup
  (dda-crate/create-server-spec dda-backup-crate))
