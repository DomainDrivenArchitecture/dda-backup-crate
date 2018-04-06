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
   [dda.pallet.core.infra :as core-infra]
   [dda.pallet.dda-backup-crate.infra.schema :as schema]
   [dda.pallet.dda-backup-crate.infra.backup-elements :as elements]
   [dda.pallet.dda-backup-crate.infra.local-management :as local]
   [dda.pallet.dda-backup-crate.infra.transport-management :as transport]))

(def facility :dda-backup)

(def ResolvedBackupConfig schema/ResolvedBackupConfig)

(def InfraResult {facility ResolvedBackupConfig})

(s/defn ^:always-validate init
  "init package-sources & update packages."
  [config :- ResolvedBackupConfig]
  (let [{:keys [transport-management]} config]
    (when (contains? transport-management :duplicity-push)
      (transport/init))))

(s/defn ^:always-validate install
  "collected install actions for backup crate."
  [config :- ResolvedBackupConfig]
  (let [{:keys [backup-user backup-script-path backup-transport-folder
                backup-store-folder backup-restore-folder
                transport-management local-management]} config]
    (local/create-backup-directory backup-user backup-transport-folder
                  backup-store-folder backup-restore-folder)
    (local/create-script-environment backup-script-path)
    (when (contains? transport-management :duplicity-push)
      (transport/install))))

(s/defn ^:always-validate configure
  "collected configuration actions for backup crate."
  [config :- ResolvedBackupConfig]
  (let [{:keys [backup-name backup-script-path backup-transport-folder
                backup-store-folder backup-restore-folder
                service-restart backup-user backup-elements
                transport-management local-management]} config
        duplicity? (contains? transport-management :duplicity-push)]
    (elements/write-file backup-name :backup backup-script-path "10_"
                         (elements/backup-script-lines backup-name backup-transport-folder service-restart (name backup-user) backup-elements))
    (elements/write-file backup-name :restore backup-script-path nil
                         (elements/restore-script-lines duplicity? backup-script-path backup-restore-folder service-restart transport-management backup-elements))
    (elements/write-file backup-name :source-transport backup-script-path "20_"
                         (elements/transport-script-lines duplicity? backup-script-path backup-transport-folder backup-store-folder local-management backup-elements))
    (when duplicity?
      (transport/configure-duplicity backup-user backup-script-path
       backup-transport-folder backup-restore-folder (:duplicity-push transport-management)))))

(defmethod core-infra/dda-init facility [core-infra config]
  (init config))

(defmethod core-infra/dda-install facility [core-infra config]
  (install config))

(defmethod core-infra/dda-configure facility [core-infra config]
  (configure config))

(def dda-backup-crate
  (core-infra/make-dda-crate-infra
   :facility facility))

(def with-backup
  (core-infra/create-infra-plan dda-backup-crate))
