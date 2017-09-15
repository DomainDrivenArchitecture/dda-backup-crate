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

(ns dda.pallet.dda-backup-crate.infra.schema
  (:require
   [schema.core :as s]))

(def BackupElementType
  "The backup source elements"
  (s/enum :mysql :file-compressed :file-plain :rsync))

(def TransportType
  (s/enum :ssh-pull :duplicity-push))

(def ScriptType
  "The backup elements"
  (s/enum :backup :restore :source-transport))

(def DuplicityAction
  (s/enum :full :incr :verify :collection-status :list-current-files :restore :remove-older-than :remove-all-but-n-full :remove-all-inc-of-but-n-full :cleanup))

(def DuplicityOptions
  "Options are seq of option-keywords and string-param or true"
  ;TODO: find schema that tests for this specifically (not pair, not one)
  [(s/either s/Keyword s/Str s/Bool)])

(def BackupBaseElement
  {:type BackupElementType
   :name s/Str
   :backup-script-name s/Str
   :backup-file-prefix-pattern s/Str
   :type-name s/Str})

(def BackupElement
  "The backup elements"
  (s/conditional
   #(= (:type %) :mysql)
   (merge
    BackupBaseElement
    {:db-user-name s/Str
     :db-user-passwd s/Str
     :db-name s/Str
     (s/optional-key :db-create-options) s/Str
     (s/optional-key :db-pre-processing) [s/Str]
     (s/optional-key :db-post-processing) [s/Str]})
   #(= (:type %) :file-compressed)
   (merge
    BackupBaseElement
    {:root-dir s/Str
     :subdir-to-save s/Str
     (s/optional-key :new-owner) s/Str})))

(def LocalManagement
  {:backup-store-folder s/Str
   :gens-stored-on-source-system s/Num})

(def TransportManagement
  {(s/optional-key :ssh-pull) s/Any
   (s/optional-key :duplicity-push)
   {:tmp-dir s/Str
    :passphrase s/Str
    :gpg-key-id s/Str
    (s/optional-key :target-s3) {:aws-access-key-id s/Str
                                 :aws-secret-access-key s/Str
                                 :s3-use-sigv4 s/Str
                                 :url s/Str}}})

(def BackupConfig
  {:backup-name s/Str
   :backup-script-path s/Str
   :backup-user s/Keyword
   (s/optional-key :service-restart) s/Str
   :local-management LocalManagement
   :transport-management TransportManagement
   :backup-elements [BackupElement]})
