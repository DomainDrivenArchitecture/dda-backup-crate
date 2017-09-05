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

(ns dda.pallet.dda-backup-crate.domain
  (:require
   [schema.core :as s]
   [dda.pallet.dda-user-crate.domain :as user]
   [dda.config.commons.map-utils :as map-utils]
   [dda.pallet.dda-backup-crate.infra :as infra]))

(def BackupDomainConfig infra/BasicBackupConfig)

(def InfraResult {infra/facility infra/BackupConfig})

(s/defn ^:allways-validate infra-configuration :- InfraResult
  [user-config :- user/UserDomainConfig
   domain-config :- BackupDomainConfig]
  (let [first (first user-config)
        user (name (key first))
        psswd (:encrypted-password (val first))
        backup-user {:backup-user {:name user
                                   :encrypted-passwd psswd}}]
    {infra/facility (merge backup-user domain-config)}))
