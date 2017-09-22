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
   [dda.config.commons.map-utils :as mu]
   [clj-pgp.core :as pgp]
   [dda.pallet.dda-user-crate.domain :as user]
   [dda.pallet.dda-backup-crate.domain.schema :as schema]
   [dda.pallet.dda-backup-crate.domain.file-convention :as file]
   [dda.pallet.dda-backup-crate.infra :as infra]
   [dda.pallet.dda-backup-crate.infra.schema :as infra-schema]))

(def BackupConfig schema/BackupConfig)

(def InfraResult infra/InfraResult)

(def default-user-config {:dataBackupSource {:encrypted-password  "WIwn6jIUt2Rbc"}})

(defn key-id
  [ascii-armored-key]
  (str (pgp/hex-id (pgp/decode-public-key ascii-armored-key)))

  (s/defn ^:always-validate infra-backup-element :- infra-schema/BackupElement
    [backup-element :- schema/BackupElement]
    (let [{:keys [name type]} backup-element]
      (merge
        backup-element
        {:backup-script-name (file/backup-file-name name type)
         :backup-file-prefix-pattern (file/backup-file-prefix-pattern name type)
         :type-name (file/element-type-name type)}))))

(s/defn ^:always-validate infra-config :- infra-schema/BackupConfig
  [config :- BackupConfig]
  (let [{:keys [backup-user transport-management backup-elements]} config
        user-key :dda-backup
        users-public-gpg (get-in backup-user
                            [:gpg :trusted-key :public-key])]
    (mu/deep-merge
      config
      {:backup-script-path "/usr/lib/dda-backup/"
       :backup-transport-folder "/var/backups/transport-outgoing"
       :backup-store-folder "/var/backups/store"
       :backup-restore-folder "/var/backups/restore"
       :backup-user user-key
       :transport-management {:duplicity-push {:tmp-dir "/tmp"
                                               :passphrase (get-in backup-user
                                                                   [:gpg :trusted-key :passphrase])
                                               :gpg-key-id (key-id backup-user)
                                               :days-stored-on-backup 21}}
       :backup-elements (map infra-backup-element backup-elements)})))



(s/defn ^:allways-validate infra-configuration :- InfraResult
  [config :- BackupConfig]
  (let [{:keys [backup-user]} config]
    (merge
     (user/infra-configuration backup-user)
     {infra/facility (infra-config config)})))

{:backup-name "name"
 :backup-script-path "/usr/lib/dda-backup/"
 :backup-transport-folder "/var/backups/transport-outgoing"
 :backup-store-folder "/var/backups/store"
 :backup-restore-folder "/var/backups/restore"
 :backup-user :x ;(key (first backup-user))
 :local-management {:gens-stored-on-source-system 3}
 :transport-management {:duplicity-push {:gpg-key-id ""
                                         :days-stored-on-backup 21
                                         :target-s3 {:aws-access-key-id ""
                                                     :aws-secret-access-key ""
                                                     :bucket-name "xxx"}}}
 :backup-elements [{:type :file-compressed
                    :backup-script-name (file/backup-file-name "name" :file-plain)
                    :backup-file-prefix-pattern (file/backup-file-prefix-pattern "name" :file-plain)
                    :type-name (file/element-type-name :file-plain)
                    :name "ssh"
                    :root-dir "/etc/"
                    :subdir-to-save "ssh"}]}
