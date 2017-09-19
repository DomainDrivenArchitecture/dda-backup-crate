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
   [dda.config.commons.map-utils :as map-utils]
   [dda.pallet.dda-user-crate.domain :as user]
   [dda.pallet.dda-backup-crate.domain.schema :as schema]
   [dda.pallet.dda-backup-crate.domain.file-convention :as file]
   [dda.pallet.dda-backup-crate.infra :as infra]))

(def BackupConfig schema/BackupConfig)

(def InfraResult infra/InfraResult)

(def default-user-config {:dataBackupSource {:encrypted-password  "WIwn6jIUt2Rbc"}})

(s/defn infra-config
  [config :- BackupConfig]
  (let [{:keys [backup-user]} config
        name "ssh"]
    {:backup-name name
     :backup-script-path "/usr/lib/dda-backup/"
     :backup-transport-folder "/var/backups/transport-outgoing"
     :backup-store-folder "/var/backups/store"
     :backup-restore-folder "/var/backups/restore"
     :backup-user (key (first backup-user))
     :local-management {:gens-stored-on-source-system 3}
     :transport-management {:duplicity-push {:tmp-dir "/tmp"
                                             :passphrase "passphrase"
                                             :gpg-key-id ""
                                             :days-stored-on-backup 21
                                             :target-s3 {:aws-access-key-id ""
                                                         :aws-secret-access-key ""
                                                         :s3-use-sigv4 ""
                                                         :url ""}}}
     :backup-elements [{:type :file-compressed
                        :backup-script-name (file/backup-file-name name :file-plain)
                        :backup-file-prefix-pattern (file/backup-file-prefix-pattern name :file-plain)
                        :type-name (file/element-type-name :file-plain)
                        :name "ssh"
                        :root-dir "/etc/"
                        :subdir-to-save "ssh"}]}))

(s/defn ^:allways-validate infra-configuration :- InfraResult
  [config :- BackupConfig]
  (let [{:keys [backup-user]} config]
    (merge
     (user/infra-configuration backup-user)
     {infra/facility (infra-config config)})))
