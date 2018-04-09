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
    [dda.pallet.dda-backup-crate.domain.schema :as schema]
    [dda.pallet.dda-backup-crate.domain.file-convention :as file]
    [dda.pallet.dda-backup-crate.infra :as infra]
    [dda.pallet.dda-backup-crate.infra.schema :as infra-schema]))

(def BackupConfig schema/BackupConfig)

(def BackupConfigResolved schema/BackupConfigResolved)

(def BackupElementResolved schema/BackupElementResolved)

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

; TODO: move to downstream namespace as that should not be exposed to outside.
(defn
  create-backup-path
  [backup-element]
  (if (contains? (:backup-path backup-element) :root-dir)
    (update backup-element :backup-path [(map #(str (-> backup-element :backup-path :root-dir) %) (-> backup-element :backup-path :subdir-to-save))])
    backup-element))

(s/defn ^:always-validate
  infra-backup-element :- infra-schema/BackupElement
  [backup-element :- BackupElementResolved]
  (let [{:keys [name type]} backup-element]
    (merge
      (create-backup-path backup-element)
      {:backup-file-name           (file/backup-file-name name type)
       :backup-file-prefix-pattern (file/backup-file-prefix-pattern name type)
       :type-name                  (file/element-type-name type)})))

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
