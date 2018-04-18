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
    [clj-pgp.core :as pgp]
    [dda.config.commons.map-utils :as mu]
    [dda.config.commons.directory-model :as directory-model]
    [dda.pallet.commons.secret :as secret]
    [dda.pallet.dda-backup-crate.domain.backup-element-type :as element-type]
    [dda.pallet.dda-user-crate.domain :as user]
    [dda.pallet.dda-backup-crate.infra :as infra]
    [dda.pallet.dda-backup-crate.infra.schema :as infra-schema]))

(def BackupElementType element-type/BackupElementType)

(def TransportType
  (s/enum :ssh-pull :duplicity-push))

(def BackupBaseElement
  {:type BackupElementType
   :name s/Str})

(def BackupDbElement
  "The db backup elements"
  {:db-user-name s/Str
   :db-user-passwd secret/Secret
   :db-name s/Str
   (s/optional-key :db-create-options) s/Str
   (s/optional-key :db-pre-processing) [s/Str]
   (s/optional-key :db-post-processing) [s/Str]})

(def BackupPath
    {:backup-path [directory-model/NonRootDirectory]
     (s/optional-key :new-owner) s/Str})

(def BackupElement
  "The backup elements"
  (s/conditional
   #(= (:type %) :mysql)
   (merge
    BackupBaseElement
    BackupDbElement)
   #(= (:type %) :file-compressed)
   (merge
    BackupBaseElement
    BackupPath)
   #(= (:type %) :file-plain)
   (merge
     BackupBaseElement
     BackupPath)))

(def LocalManagement
  {:gens-stored-on-source-system s/Num})

(def TransportManagement
  {(s/optional-key :ssh-pull) s/Any
   (s/optional-key :duplicity-push)
   {:public-key secret/Secret
    :private-key secret/Secret
    :passphrase secret/Secret
    (s/optional-key :target-s3) {:aws-access-key-id secret/Secret
                                 :aws-secret-access-key secret/Secret
                                 :bucket-name s/Str
                                 (s/optional-key :directory-name) s/Str}}})

(def BackupConfig
  {:backup-name s/Str
   :backup-user user/User
   (s/optional-key :service-restart) s/Str
   :local-management LocalManagement
   :transport-management TransportManagement
   :backup-elements [BackupElement]})

(def UserResolved (secret/create-resolved-schema user/User))

(def TransportManagementResolved (secret/create-resolved-schema TransportManagement))

(def BackupElementResolved (secret/create-resolved-schema BackupElement))

(def BackupConfigResolved (secret/create-resolved-schema BackupConfig))

(def InfraResult infra/InfraResult)

; TODO: Wire hardcoded password through domain config
(def default-user-config {:dataBackupSource {:hashed-password {:plain "WIwn6jIUt2Rbc"}}})

(defn key-id
  [ascii-armored-key]
  (clojure.string/upper-case (pgp/hex-id (pgp/decode-public-key ascii-armored-key))))

; TODO: Wire hardcoded password through domain config
(s/defn ^:always-validate
  user-domain-configuration
  [config :- BackupConfigResolved]
  (let [{:keys [backup-user transport-management]} config
        public-gpg (get-in transport-management [:duplicity-push :public-key])
        private-gpg (get-in transport-management [:duplicity-push :private-key])
        passphrase (get-in transport-management [:duplicity-push :passphrase])]
    (merge
      (if (contains? transport-management :duplicity-push)
        {:root {:hashed-password "fksdjfiosjfr8o0jterojdo"
                :gpg             {:trusted-key {:public-key  public-gpg
                                                :private-key private-gpg
                                                :passphrase  passphrase}}}}
        {})
      {:dda-backup backup-user})))

(s/defn ^:always-validate
  infra-backup-element :- infra-schema/BackupElement
  [backup-element :- BackupElementResolved]
  (let [{:keys [name type]} backup-element]
    (-> backup-element
        (assoc :backup-file-name (element-type/backup-file-name name type))
        (assoc :backup-file-prefix-pattern (str "/var/backups/transport-outgoing/" (element-type/backup-file-prefix-pattern name type)))
        (assoc :type-name  (element-type/element-type-name type)))))

(s/defn ^:always-validate
  infra-config :- infra-schema/ResolvedBackupConfig
  [config :- BackupConfigResolved]
  (let [{:keys [backup-user transport-management backup-elements]} config
        user-key :dda-backup
        public-gpg (get-in transport-management
                           [:duplicity-push :public-key])
        additional-map (if (contains? transport-management :duplicity-push)
                         {:transport-management {:duplicity-push {:tmp-dir               "/tmp"
                                                                  :gpg-key-id            (key-id public-gpg)
                                                                  :days-stored-on-backup 21}}}
                         {})]
    (mu/deep-merge
      config
      additional-map
      {:backup-script-path      "/usr/local/lib/dda-backup/"
       :backup-transport-folder "/var/backups/transport-outgoing"
       :backup-store-folder     "/var/backups/store"
       :backup-restore-folder   "/var/backups/restore"
       :backup-user             user-key
       :backup-elements         (map infra-backup-element backup-elements)})))


(s/defn ^:always-validate
  infra-configuration :- InfraResult
  [config :- BackupConfigResolved]
  (let [{} config]
    {infra/facility (infra-config config)}))
