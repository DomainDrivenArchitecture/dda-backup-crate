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

(ns dda.pallet.dda-backup-crate.domain.schema
  (:require
    [schema.core :as s]
    [dda.config.commons.secret :as secret]
    [dda.pallet.dda-user-crate.domain :as user]
    [dda.pallet.dda-backup-crate.infra.schema :as infra]
    [dda.config.commons.directory-model :as directory-model]))

(def BackupElementType infra/BackupElementType)

(def TransportType infra/TransportType)

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
  {:backup-path
   (s/either [directory-model/NonRootDirectory]
             {:root-dir directory-model/NonRootDirectory
              :subdir-to-save [directory-model/NonRootDirectory]})
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

(def BackupElementResolved (secret/create-resolved-schema BackupElement))

(def BackupConfigResolved (secret/create-resolved-schema BackupConfig))
