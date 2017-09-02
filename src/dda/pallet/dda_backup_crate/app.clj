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

(ns dda.pallet.dda-backup-crate.app
  (:require
   [schema.core :as s]
   [dda.cm.group :as group]
   [dda.pallet.core.dda-crate :as dda-crate]
   [dda.config.commons.map-utils :as mu]
   [dda.pallet.dda-user-crate.app :as user]
   [dda.pallet.dda-config-crate.infra :as config-crate]
   [dda.pallet.dda-backup-crate.infra :as infra]
   [dda.pallet.dda-backup-crate.domain :as domain]))

(def with-backup infra/with-backup)

(def InfraResult domain/InfraResult)

(def BackupAppConfig
  {:group-specific-config
   {s/Keyword (merge
               InfraResult
               user/InfraResult)}})

(s/defn ^:allways-validate create-app-configuration :- BackupAppConfig
  [config :- infra/BackupConfig
   group-key :- s/Keyword]
  {:group-specific-config
     {group-key config}})

(defn app-configuration
  [user-config domain-config & {:keys [group-key] :or {group-key :dda-backup-group}}]
  (s/validate domain/BackupDomainConfig domain-config)
  (mu/deep-merge
    (user/app-configuration user-config :group-key group-key)
  (create-app-configuration (domain/infra-configuration user-config domain-config) group-key)))

(s/defn ^:always-validate backup-group-spec
  [app-config :- BackupAppConfig]
  (group/group-spec
    app-config [(config-crate/with-config app-config)
                user/with-user
                with-backup]))
